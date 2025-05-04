package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author shoqapik
 */
public class CategoryButton extends SmithStateSwitchingButton{
    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");

    private BteRecipeCategory category;
    public CategoryButton(BteRecipeCategory category) {
        super(0, 0, 35, 27, false);
        this.category = category;
        this.initTextureValues(88, 180, 35, 0, CRAFTING_TABLE_LOCATION);
    }

    @Override
    public void renderButton(PoseStack p_94631_, int p_94632_, int p_94633_, float p_94634_) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        RenderSystem.disableDepthTest();
        int i = this.xTexStart;
        int j = this.yTexStart;
        if (this.isStateTriggered) {
            i += this.xDiffTex;
        }

        if (this.isHoveredOrFocused()) {
            j += this.yDiffTex;
        }

        int k = this.x;
        if (this.isStateTriggered) {
            k -= 2;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(p_94631_, k, this.y, i, j, this.width, this.height, 512,512);
        RenderSystem.enableDepthTest();
        this.renderIcon(minecraft.getItemRenderer());

    }

    private void renderIcon(ItemRenderer p_100454_) {
        int i = this.isStateTriggered ? -2 : 0;
        p_100454_.renderAndDecorateFakeItem(category.item, this.x + 9+i, this.y + 5);
    }

    public BteRecipeCategory getCategory() {
        return category;
    }
}
