package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.BteRecipeBookComponent;
import fr.shoqapik.btemobs.menu.BteAbstractCraftMenu;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class BteAbstractCraftScreen<T extends BteAbstractCraftMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
    protected final BteRecipeBookComponent recipeBookComponent = new BteRecipeBookComponent();
    protected Button craftButton;
    protected boolean widthTooNarrow;

    // EditBox propio, igual que ExplorerTableScreen/WarlockCraftScreen/DruidScreen.
    // No usamos el EditBox interno del RecipeBookComponent porque otros mods
    // (Fabrication) lo interfieren y su sistema de foco no funciona correctamente.
    private EditBox searchBox;
    private String previousSearch = "";


    public BteAbstractCraftScreen(T containerMenu, Inventory inventory, Component component) {
        super(containerMenu, inventory, component);
    }

    public abstract ResourceLocation getTexture();

    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;

        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.recipeBookComponent.setVisible(true);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);



        // Posicion del searchBox basada en leftPos (ya calculado con widthTooNarrow)
        // El panel del libro esta 147px a la izquierda de leftPos
        // Crear searchBox con posicion temporal; se actualizara en el primer render
        // cuando el box interno ya tenga sus coordenadas definitivas
        String prevValue = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, 0, 0, 80, 14,
                Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setValue(prevValue);
        this.addWidget(this.searchBox);
        // Sincronizar posicion con el box interno inmediatamente
        syncSearchBoxPosition();

        this.craftButton = this.addRenderableWidget(new Button(this.leftPos + 134, (this.height / 2 - this.imageHeight / 2) + 68, 35, 14, Component.literal("Craft"), new Button.OnPress() {
            @Override
            public void onPress(Button button) {
                Recipe<?> recipe = BteAbstractCraftScreen.this.recipeBookComponent.recipeBookPage.getLastClickedRecipe();
                if (recipe == null) {
                    for (RecipeButton b : recipeBookComponent.recipeBookPage.buttons) {
                        if (menu.recipeMatches((Recipe<? super BteAbstractCraftContainer>) b.getRecipe())) {
                            recipe = b.getRecipe();
                            BteMobsMod.LOGGER.debug("[BteAbstractCraftScreen] No recipe clicked, using first matching: {}", recipe.getId());
                            break;
                        }
                    }
                } else {
                    if (!menu.recipeMatches((Recipe<? super BteAbstractCraftContainer>) recipe)) {
                        BteMobsMod.LOGGER.debug("[BteAbstractCraftScreen] Selected recipe {} no longer matches, aborting", recipe.getId());
                        return;
                    }
                }
                if (recipe == null) return;
                BteMobsMod.LOGGER.debug("[BteAbstractCraftScreen] Crafting: {}", recipe.getId());
                BteAbstractCraftScreen.this.menu.craftItemClient(recipe);
                BteAbstractCraftScreen.this.craftButton.active = false;
            }
        }));
        this.craftButton.active = false;
        this.addWidget(this.recipeBookComponent);
        this.setInitialFocus(this.recipeBookComponent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            // Bloquear E para que no cierre el menu mientras se escribe
            return true;
        }
        if (this.recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.charTyped(codePoint, modifiers)) return true;
        }
        if (this.recipeBookComponent.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.recipeBookComponent.tick();
        this.searchBox.tick();

        // Sincronizar nuestro searchBox con el interno del RecipeBookComponent
        // para que el filtrado de recetas funcione
        String current = this.searchBox.getValue();
        if (!current.equals(previousSearch)) {
            previousSearch = current;
            syncSearchToRecipeBook(current);
        }
    }

    /**
     * Escribe el texto de busqueda en el EditBox interno del RecipeBookComponent
     * via reflexion, para que el filtrado de recetas funcione correctamente.
     */
    private void syncSearchToRecipeBook(String text) {
        EditBox internalBox = BteRecipeBookComponent.getInternalBox(this.recipeBookComponent);
        if (internalBox != null) {
            // Usar espacio cuando vacio para que suppressVanillaHint en BteRecipeBookComponent
            // no resetee el valor y el filtrado funcione (RecipeBookComponent usa trim())
            internalBox.setValue(text.isEmpty() ? " " : text);
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        // Mantener la posicion del searchBox sincronizada con el panel del libro
        syncSearchBoxPosition();
        this.recipeBookComponent.setVisible(true);
        this.renderBackground(pPoseStack);
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.renderBg(pPoseStack, pPartialTick, pMouseX, pMouseY);
            this.recipeBookComponent.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        } else {
            this.recipeBookComponent.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            this.recipeBookComponent.renderGhostRecipe(pPoseStack, this.leftPos, this.topPos, false, pPartialTick);
        }

        // Renderizar nuestro propio searchBox encima del panel del libro
        if (!this.searchBox.isFocused() && this.searchBox.getValue().isEmpty()) {
            // Hint "Search..." en las mismas coordenadas que el box
            drawString(pPoseStack, this.minecraft.font, "Search...",
                    this.searchBox.x + 2, this.searchBox.y + 2, 0xFF808080);
        }
        this.searchBox.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        this.renderTooltip(pPoseStack, pMouseX, pMouseY);
        this.recipeBookComponent.renderTooltip(pPoseStack, this.leftPos, this.topPos, pMouseX, pMouseY);
    }

    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pX, int pY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getTexture());
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(pPoseStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack p_97808_, int p_97809_, int p_97810_) {}

    @Override
    protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(pX, pY, pWidth, pHeight, pMouseX, pMouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.searchBox.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.setFocused(this.searchBox);
            return true;
        }
        if (this.recipeBookComponent.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        if (craftButton.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
        boolean flag = pMouseX < (double)pGuiLeft || pMouseY < (double)pGuiTop || pMouseX >= (double)(pGuiLeft + this.imageWidth) || pMouseY >= (double)(pGuiTop + this.imageHeight);
        return this.recipeBookComponent.hasClickedOutside(pMouseX, pMouseY, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, pMouseButton) && flag;
    }

    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        this.recipeBookComponent.slotClicked(pSlot);
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookComponent.recipesUpdated();
    }

    @Override
    public void removed() {
        this.recipeBookComponent.removed();
        super.removed();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookComponent;
    }

    public void setCraftButtonActive(boolean active) {
        this.craftButton.active = active;
    }

    /**
     * Lee el campo xOffset de RecipeBookComponent via reflexion.
     * Este es el mismo valor que usa el vanilla para posicionar el panel del libro,
     * asi que usarlo garantiza que nuestro searchBox queda alineado en cualquier resolucion.
     */
    /**
     * Copia la posicion x,y del searchBox interno del RecipeBookComponent al nuestro.
     * El box interno tiene las coordenadas correctas para cualquier resolucion
     * porque vanilla las calcula en init(). Nosotros simplemente las usamos.
     */
    private void syncSearchBoxPosition() {
        if (this.searchBox == null) return;
        net.minecraft.client.gui.components.EditBox internalBox =
            BteRecipeBookComponent.getInternalBox(this.recipeBookComponent);
        if (internalBox != null) {
            this.searchBox.x = internalBox.x;
            this.searchBox.y = internalBox.y;
        } else {
            // Fallback: misma formula que ExplorerTableScreen
            this.searchBox.x = (this.width - 147) / 2 - 86 + 28;
            this.searchBox.y = (this.height - 166) / 2 + 14;
        }
    }
}
