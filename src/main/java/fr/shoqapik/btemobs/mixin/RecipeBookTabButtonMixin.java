package fr.shoqapik.btemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen;
import fr.shoqapik.btemobs.client.widget.CategoryButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pestanas del recipe book de la mesa de Anna mas estrechas, igual que las CategoryButton del resto de interfaces de
 * crafteo de los NPCs: se dibujan CategoryButton.SHRINK px mas a la derecha recortando columnas del relleno, y el icono
 * se mueve la mitad. Solo en las pantallas BteAbstractCraftScreen; el recipe book vanilla no cambia.
 */
@Mixin(RecipeBookTabButton.class)
public class RecipeBookTabButtonMixin {

    private static boolean btemobs$narrowTabs() {
        return Minecraft.getInstance().screen instanceof BteAbstractCraftScreen;
    }

    @Redirect(method = "renderButton",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookTabButton;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
    private void btemobs$narrowTabBackground(RecipeBookTabButton self, PoseStack poseStack, int x, int y, int u, int v, int width, int height) {
        if (!btemobs$narrowTabs()) {
            self.blit(poseStack, x, y, u, v, width, height);
            return;
        }
        int shrink = CategoryButton.SHRINK;
        int edge = CategoryButton.LEFT_EDGE;
        self.blit(poseStack, x + shrink, y, u, v, edge, height);
        self.blit(poseStack, x + shrink + edge, y, u + edge + shrink, v, width - edge - shrink, height);
    }

    @ModifyArg(method = "renderIcon",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderAndDecorateFakeItem(Lnet/minecraft/world/item/ItemStack;II)V"),
            index = 1)
    private int btemobs$narrowTabIcon(int x) {
        return btemobs$narrowTabs() ? x + CategoryButton.SHRINK / 2 : x;
    }
}
