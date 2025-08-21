//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package fr.shoqapik.btemobs.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeButton.class)
public class RecipeButtonMixin {

    @Redirect(
            method = {"renderButton"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderAndDecorateFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
            )
    )
    private void renderAndDecorateItem(ItemRenderer instance, ItemStack pStack, int pX, int pY) {
        if (Minecraft.getInstance().screen instanceof BteAbstractCraftScreen) {
            if (pStack.hasTag() && pStack.getTag().contains("texture")) {
                ResourceLocation texture = new ResourceLocation(pStack.getTag().getString("texture"));
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();
                buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buffer.vertex(pX, pY + 16, 0.0).uv(0.0F, 1.0F).endVertex();
                buffer.vertex(pX + 16, pY + 16, 0.0).uv(1.0F, 1.0F).endVertex();
                buffer.vertex(pX + 16, pY, 0.0).uv(1.0F, 0.0F).endVertex();
                buffer.vertex(pX, pY, 0.0).uv(0.0F, 0.0F).endVertex();
                tesselator.end();
                return;
            }

            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(pStack, pX, pY);
        }

    }
}
