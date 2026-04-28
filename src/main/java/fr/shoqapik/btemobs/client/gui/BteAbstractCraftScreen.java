package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.BteRecipeBookComponent;
import fr.shoqapik.btemobs.menu.BteAbstractCraftMenu;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import net.minecraft.client.gui.components.Button;
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
    protected final RecipeBookComponent recipeBookComponent = new BteRecipeBookComponent();
    protected Button craftButton;

    protected boolean widthTooNarrow;

    public BteAbstractCraftScreen(T containerMenu, Inventory inventory, Component component) {
        super(containerMenu, inventory, component);
    }

    public abstract ResourceLocation getTexture();

    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;

        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        // FIX: setVisible(true) debe llamarse ANTES de updateScreenPosition.
        // updateScreenPosition desplaza leftPos a la derecha solo si isVisible()==true.
        // En la primera apertura isVisible()==false -> leftPos queda centrado ->
        // el panel de recetas se superpone al inventario del jugador.
        // En aperturas posteriores el estado visible=true persiste -> funciona bien.
        this.recipeBookComponent.setVisible(true);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);

        this.craftButton = this.addRenderableWidget(new Button(this.leftPos + 134, (this.height / 2 - this.imageHeight / 2) + 68, 35, 14, Component.literal("Craft"), new Button.OnPress() {
            @Override
            public void onPress(Button button) {
                // Obtener la receta en la que el jugador hizo clic explícitamente
                Recipe<?> recipe = BteAbstractCraftScreen.this.recipeBookComponent.recipeBookPage.getLastClickedRecipe();

                // BUG FIX: El bucle anterior sobreescribía 'recipe' con la primera receta
                // que matcheara en la página, ignorando la selección del jugador.
                // Esto causaba que al crafteear iron_leggings (7 hierros) se obtuviera
                // anchor (5 hierros) porque anchor aparecía antes en la página y también
                // satisfacía el check de ingredientes.
                //
                // Corrección: solo usar el bucle como fallback cuando recipe == null,
                // es decir, cuando el jugador no ha hecho clic en ninguna receta aún.
                if (recipe == null) {
                    for (RecipeButton b : recipeBookComponent.recipeBookPage.buttons) {
                        if (menu.recipeMatches((Recipe<? super BteAbstractCraftContainer>) b.getRecipe())) {
                            recipe = b.getRecipe();
                            BteMobsMod.LOGGER.debug("[BteAbstractCraftScreen] No recipe clicked, using first matching: {}", recipe.getId());
                            break;
                        }
                    }
                } else {
                    // Validar que la receta seleccionada sigue siendo crafteable
                    if (!menu.recipeMatches((Recipe<? super BteAbstractCraftContainer>) recipe)) {
                        BteMobsMod.LOGGER.debug("[BteAbstractCraftScreen] Selected recipe {} no longer matches, aborting", recipe.getId());
                        return;
                    }
                }

                if (recipe == null) {
                    return;
                }

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
        if (this.recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.recipeBookComponent.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.recipeBookComponent.tick();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
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
    protected void renderLabels(PoseStack p_97808_, int p_97809_, int p_97810_) {
    }

    @Override
    protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(pX, pY, pWidth, pHeight, pMouseX, pMouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.recipeBookComponent.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }
        if (craftButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }
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
}
