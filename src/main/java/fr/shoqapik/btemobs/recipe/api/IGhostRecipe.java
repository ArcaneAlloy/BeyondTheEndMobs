package fr.shoqapik.btemobs.recipe.api;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

public interface IGhostRecipe {
    void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots);
}
