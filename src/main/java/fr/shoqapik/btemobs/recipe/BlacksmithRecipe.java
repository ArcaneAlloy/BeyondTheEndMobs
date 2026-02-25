package fr.shoqapik.btemobs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.util.Locale;


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

    public static class Serializer implements RecipeSerializer<BlacksmithRecipe> {

        @Override
        public BlacksmithRecipe fromJson(ResourceLocation recipeId, JsonObject json) {

            String categoryName = GsonHelper.getAsString(json, "category");
            BteRecipeCategory category;

            try {
                category = BteRecipeCategory.valueOf(categoryName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid recipe category: " + categoryName);
            }

            int tier = GsonHelper.getAsInt(json, "tier", 0);

            NonNullList<Ingredient> ingredients = NonNullList.create();
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");

            for (int i = 0; i < array.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(array.get(i));
                if (!ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }

            if (ingredients.isEmpty()) {
                throw new JsonParseException("No ingredients for blacksmith recipe");
            }

            ItemStack result = CraftingHelper.getItemStack(
                    GsonHelper.getAsJsonObject(json, "result"),
                    true,
                    true
            );

            return new BlacksmithRecipe(recipeId, category, tier, ingredients, result);
        }

        @Override
        public BlacksmithRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {

            String categoryName = buffer.readUtf();
            BteRecipeCategory category = BteRecipeCategory.valueOf(categoryName);

            int tier = buffer.readInt();

            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack result = buffer.readItem();

            return new BlacksmithRecipe(recipeId, category, tier, ingredients, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BlacksmithRecipe recipe) {

            buffer.writeUtf(recipe.category.name());
            buffer.writeInt(recipe.tier);

            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.result);
        }
    }

    @Override
    public NonNullList<Ingredient> getIngredients(){
        return ingredients;
    }
}
