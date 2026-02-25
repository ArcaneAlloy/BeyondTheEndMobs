package fr.shoqapik.btemobs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.util.Locale;

public class BlacksmithUpgradeRecipe extends BteAbstractRecipe {

    public final Ingredient base;

    public BlacksmithUpgradeRecipe(ResourceLocation resourceLocation, BteRecipeCategory category, Ingredient base, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(resourceLocation, category, ingredients, result);
        this.base = base;
    }

    @Override
    public ItemStack assemble(BteAbstractCraftContainer pInv) {
        ItemStack itemstack = this.result.copy();
        CompoundTag compoundtag = pInv.getItem(0).getTag();
        if (compoundtag != null) {
            itemstack.setTag(compoundtag.copy());
        }

        return itemstack;
    }


    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.BLACKSMITH_UPGRADE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<BlacksmithUpgradeRecipe> {

        @Override
        public BlacksmithUpgradeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {

            String categoryName = GsonHelper.getAsString(json, "category");
            BteRecipeCategory category = BteRecipeCategory.valueOf(categoryName.toUpperCase(Locale.ROOT));

            Ingredient base = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "base"));

            NonNullList<Ingredient> ingredients = NonNullList.create();
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");

            for (int i = 0; i < array.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(array.get(i));
                if (!ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }

            ItemStack result = CraftingHelper.getItemStack(
                    GsonHelper.getAsJsonObject(json, "result"),
                    true,
                    true
            );

            return new BlacksmithUpgradeRecipe(recipeId, category, base, ingredients, result);
        }

        @Override
        public BlacksmithUpgradeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {

            BteRecipeCategory category = BteRecipeCategory.valueOf(buffer.readUtf());

            Ingredient base = Ingredient.fromNetwork(buffer);

            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack result = buffer.readItem();

            return new BlacksmithUpgradeRecipe(recipeId, category, base, ingredients, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BlacksmithUpgradeRecipe recipe) {

            buffer.writeUtf(recipe.category.name());

            recipe.base.toNetwork(buffer);

            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.result);
        }
    }
}
