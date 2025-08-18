package fr.shoqapik.btemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GhostRecipe.class)
public class GhostRecipeMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthFunc(I)V", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    public void render(PoseStack pPoseStack, Minecraft pMinecraft, int pLeftPos, int pTopPos, boolean p_100154_, float pPartialTick, CallbackInfo ci, int i, GhostRecipe.GhostIngredient ingredient, int j, int k, ItemStack itemstack, ItemRenderer itemrenderer) {
        if(itemstack.getCount() > 1) {
            itemrenderer.renderGuiItemDecorations(pMinecraft.font, itemstack, j, k);
        }
    }

}
