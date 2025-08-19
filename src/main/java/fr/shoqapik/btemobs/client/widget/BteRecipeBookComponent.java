package fr.shoqapik.btemobs.client.widget;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;

import java.util.Iterator;
import java.util.List;

public class BteRecipeBookComponent extends RecipeBookComponent {

    @Override
    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);
        if(pRecipe instanceof UpgradeRecipe) {
            UpgradeRecipe recipe = (UpgradeRecipe) pRecipe;
            this.ghostRecipe.addIngredient(recipe.base, (pSlots.get(0)).x, (pSlots.get(0)).y);
            this.ghostRecipe.addIngredient(recipe.addition, (pSlots.get(1)).x, (pSlots.get(1)).y);
        }
        for (int i = 0 ; i < pRecipe.getIngredients().size() ; i++){
            if(this.menu.getSlot(i).getItem().isEmpty()){
                this.ghostRecipe.addIngredient(pRecipe.getIngredients().get(i),pSlots.get(i).x,pSlots.get(i).y);
            }
        }
    }
}
