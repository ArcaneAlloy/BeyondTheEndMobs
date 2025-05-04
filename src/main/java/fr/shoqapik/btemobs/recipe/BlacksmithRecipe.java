package fr.shoqapik.btemobs.recipe;

import com.google.gson.JsonObject;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;


public class BlacksmithRecipe extends BteAbstractRecipe {

    protected final int tier;

    public BlacksmithRecipe(ResourceLocation resourceLocation, BteRecipeCategory category, int tier, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(resourceLocation, category, ingredients, result);
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
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
        public BlacksmithRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            int tier = json.get("tier").getAsInt();
            return fromJson(recipeId, json, tier);
        }

        @Override
        public BlacksmithRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer) {
            int tier = pBuffer.readInt();
            return fromNetwork(recipeId, pBuffer, tier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, BlacksmithRecipe recipe) {
            pBuffer.writeInt(recipe.tier);
            super.toNetwork(pBuffer, recipe);
        }

        @Override
        public boolean hasResultItem() {
            return true;
        }

        @Override
        protected BlacksmithRecipe of(ResourceLocation resourceLocation, BteRecipeCategory category, NonNullList<Ingredient> ingredients, ItemStack result, Object... objects) {
            return new BlacksmithRecipe(resourceLocation, category, (int)objects[0], ingredients, result);
        }
    }

    @Override
    public NonNullList<Ingredient> getIngredients(){
        return ingredients;
    }
}
