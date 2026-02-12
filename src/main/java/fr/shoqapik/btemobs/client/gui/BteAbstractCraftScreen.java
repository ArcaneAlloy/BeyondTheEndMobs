package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.client.widget.BteRecipeBookComponent;
import fr.shoqapik.btemobs.menu.BteAbstractCraftMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
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
public abstract class BteAbstractCraftScreen<T extends BteAbstractCraftMenu>
        extends AbstractContainerScreen<T>
        implements RecipeUpdateListener {

    protected final RecipeBookComponent recipeBook = new BteRecipeBookComponent();
    protected Button craftButton;

    protected boolean widthTooNarrow;

    protected BteAbstractCraftScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public abstract ResourceLocation getTexture();


    @Override
    protected void init() {
        super.init();

        this.widthTooNarrow = this.width < 379;

        initRecipeBook();
        initCraftButton();
    }

    private void initRecipeBook() {
        recipeBook.init(this.width, this.height, this.minecraft, widthTooNarrow, this.menu);
        this.leftPos = recipeBook.updateScreenPosition(this.width, this.imageWidth);

        addWidget(recipeBook);
        setInitialFocus(recipeBook);

        recipeBook.setVisible(true);
    }

    private void initCraftButton() {
        int x = this.leftPos + 134;
        int y = (this.height - this.imageHeight) / 2 + 68;

        craftButton = new Button(x, y, 35, 14, Component.literal("Craft"), btn -> {Recipe<?> recipe = recipeBook.recipeBookPage.getLastClickedRecipe();menu.craftItemClient(recipe);btn.active = false;});

        craftButton.active = false;
        addRenderableWidget(craftButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        recipeBook.tick();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        if (recipeBook.isVisible() && widthTooNarrow) {
            renderBackgroundOnly(poseStack, partialTick);
            recipeBook.render(poseStack, mouseX, mouseY, partialTick);
        } else {
            recipeBook.render(poseStack, mouseX, mouseY, partialTick);
            super.render(poseStack, mouseX, mouseY, partialTick);
            recipeBook.renderGhostRecipe(poseStack, leftPos, topPos, false, partialTick);
        }

        super.renderTooltip(poseStack, mouseX, mouseY);
        recipeBook.renderTooltip(poseStack, leftPos, topPos, mouseX, mouseY);
    }

    private void renderBackgroundOnly(PoseStack poseStack, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, getTexture());

        int y = (this.height - this.imageHeight) / 2;
        blit(poseStack, leftPos, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (craftButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (recipeBook.mouseClicked(mouseX, mouseY, button)) {
            setFocused(recipeBook);
            return true;
        }

        if (widthTooNarrow && recipeBook.isVisible()) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        boolean outsideMain = mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + imageWidth || mouseY >= guiTop + imageHeight;

        return recipeBook.hasClickedOutside(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, mouseButton) && outsideMain;
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return (!widthTooNarrow || !recipeBook.isVisible()) && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
        recipeBook.slotClicked(slot);
    }


    @Override
    public void recipesUpdated() {
        recipeBook.recipesUpdated();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return recipeBook;
    }


    @Override
    public void removed() {
        recipeBook.removed();
        super.removed();
    }


    public void setCraftButtonActive(boolean active) {
        craftButton.active = active;
    }
}