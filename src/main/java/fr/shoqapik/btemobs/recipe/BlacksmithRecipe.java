package fr.shoqapik.btemobs.recipe;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.RecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;


public class BlacksmithRecipe extends BteAbstractRecipe {

    public BlacksmithRecipe(ResourceLocation resourceLocation, RecipeCategory category, int tier, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(resourceLocation, category, tier, ingredients, result);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.BLACKSMITH_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.BLACKSMITH_RECIPE.get();
    }

    public static class Serializer extends BteAbstractRecipe.AbstractSerializer<BlacksmithRecipe> {
        @Override
        protected BlacksmithRecipe of(ResourceLocation resourceLocation, RecipeCategory category, int tier, NonNullList<Ingredient> ingredients, ItemStack result, Object... objects) {
            return new BlacksmithRecipe(resourceLocation, category, tier, ingredients, result);
        }
    }

    @Override
    public NonNullList<Ingredient> getIngredients(){
        return ingredients;
    }
}
