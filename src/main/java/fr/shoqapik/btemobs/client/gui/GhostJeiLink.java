package fr.shoqapik.btemobs.client.gui;

import fr.shoqapik.btemobs.integration.JEIPlugin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Clic sobre un ingrediente de previsualizacion (receta fantasma) de las ventanas de los NPCs: abre en JEI las recetas
 * de ese item (como pulsar R sobre el), para ver como conseguir lo que falta.
 */
public final class GhostJeiLink {

    private GhostJeiLink() {}

    /**
     * Devuelve true si el clic era sobre un ingrediente fantasma y se ha abierto JEI. Solo con clic izquierdo, sin
     * nada en el cursor y con la casilla vacia: si el jugador lleva un objeto, el clic sirve para dejarlo en la casilla.
     */
    public static boolean click(AbstractContainerScreen<?> screen, GhostRecipe ghost, int leftPos, int topPos,
                                double mouseX, double mouseY, int button) {
        if (button != 0 || ghost == null || ghost.size() == 0) return false;
        if (!ModList.get().isLoaded("jei")) return false;
        if (!screen.getMenu().getCarried().isEmpty()) return false;
        for (int i = 0; i < ghost.size(); i++) {
            GhostRecipe.GhostIngredient ingredient = ghost.get(i);
            int x = ingredient.getX() + leftPos;
            int y = ingredient.getY() + topPos;
            if (mouseX < x || mouseY < y || mouseX >= x + 16 || mouseY >= y + 16) continue;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.x == ingredient.getX() && slot.y == ingredient.getY() && slot.hasItem()) return false;
            }
            ItemStack stack = ingredient.getItem();
            if (stack.isEmpty()) return false;
            return JEIPlugin.showRecipesFor(stack.copy());
        }
        return false;
    }
}
