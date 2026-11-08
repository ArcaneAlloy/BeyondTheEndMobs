package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * Cantidades de los ingredientes de previsualizacion (receta fantasma).
 *
 * GhostRecipe de vanilla solo dibuja la cantidad del primer ingrediente (en el libro de recetas de vanilla el primero es
 * el resultado). En las ventanas de los NPCs el primero es un ingrediente mas, asi que solo salia la cantidad del
 * primero y el resto parecia pedir 1 (p. ej. el Bonebow mostraba "3 huesos" y "1 cuerda" cuando pide 3 cuerdas).
 * Esto dibuja la cantidad de los demas, en el mismo sitio y de la misma forma que vanilla dibuja la del primero.
 */
public final class GhostRecipeCounts {

    private GhostRecipeCounts() {}

    /** Llamar justo despues de ghostRecipe.render(...), con las mismas coordenadas. */
    public static void render(PoseStack poseStack, Minecraft minecraft, GhostRecipe ghost, int leftPos, int topPos) {
        if (ghost == null || minecraft == null) return;
        for (int i = 1; i < ghost.size(); i++) {
            GhostRecipe.GhostIngredient ingredient = ghost.get(i);
            ItemStack stack = ingredient.getItem();
            if (stack.isEmpty() || stack.getCount() <= 1) continue;
            minecraft.getItemRenderer().renderGuiItemDecorations(minecraft.font, stack,
                    ingredient.getX() + leftPos, ingredient.getY() + topPos);
        }
    }
}
