package fr.shoqapik.btemobs.recipe.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.util.Arrays;

public abstract class BteAbstractRecipe implements Recipe<BteAbstractCraftContainer> {

    private final ResourceLocation id;
    protected final BteRecipeCategory category;
    protected final NonNullList<Ingredient> ingredients;
    protected final ItemStack result;

    public BteAbstractRecipe(ResourceLocation id, BteRecipeCategory category, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.id = id;
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public BteRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(BteAbstractCraftContainer inventory, Level level) {
        return hasRequiredItems(inventory::countItem);
    }

    public boolean hasItems(Player player) {
        return hasRequiredItems(player.getInventory()::countItem);
    }

    private boolean hasRequiredItems(IntCountProvider countProvider) {
        for (Ingredient ingredient : ingredients) {
            boolean hasEnough = false;

            for (ItemStack stack : ingredient.getItems()) {
                if (countProvider.count(stack.getItem()) >= stack.getCount()) {
                    hasEnough = true;
                    break;
                }
            }

            if (!hasEnough) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(BteAbstractCraftContainer inventory) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @FunctionalInterface
    private interface IntCountProvider {
        int count(net.minecraft.world.item.Item item);
    }


    public static abstract class AbstractSerializer<T extends BteAbstractRecipe>
            implements RecipeSerializer<T> {

        @Override
        public T fromJson(ResourceLocation recipeId, JsonObject json) {
            return fromJson(recipeId, json, new Object[0]);
        }

        protected T fromJson(ResourceLocation recipeId,
                             JsonObject json,
                             Object... extra) {

            String categoryName = GsonHelper.getAsString(json, "category");
            BteRecipeCategory category;

            try {
                category = BteRecipeCategory.valueOf(categoryName);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid recipe category: " + categoryName);
            }

            NonNullList<Ingredient> ingredients =
                    itemsFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (ingredients.isEmpty()) {
                throw new JsonParseException("No ingredients for bte recipe");
            }

            if (ingredients.size() > 6) {
                throw new JsonParseException("Too many ingredients for bte recipe. Max: 6");
            }

            ItemStack resultStack = hasResultItem()
                    ? CraftingHelper.getItemStack(
                    GsonHelper.getAsJsonObject(json, "result"),
                    true,
                    true)
                    : ItemStack.EMPTY;

            return of(recipeId, category, ingredients, resultStack, extra);
        }

        public static NonNullList<Ingredient> itemsFromJson(JsonArray array) {
            NonNullList<Ingredient> list = NonNullList.create();

            for (int i = 0; i < array.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(array.get(i));
                if (!ingredient.isEmpty()) {
                    list.add(ingredient);
                }
            }

            return list;
        }

        @Override
        public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return fromNetwork(recipeId, buffer, new Object[0]);
        }

        protected T fromNetwork(ResourceLocation recipeId,
                                FriendlyByteBuf buffer,
                                Object... extra) {

            BteRecipeCategory category =
                    BteRecipeCategory.valueOf(buffer.readUtf());

            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients =
                    NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < size; ++i) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack resultStack =
                    hasResultItem() ? buffer.readItem() : ItemStack.EMPTY;

            return of(recipeId, category, ingredients, resultStack, extra);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            buffer.writeUtf(recipe.getCategory().name());
            buffer.writeVarInt(recipe.ingredients.size());

            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            if (hasResultItem()) {
                buffer.writeItem(recipe.result);
            }
        }

        public abstract boolean hasResultItem();

        protected abstract T of(ResourceLocation id,
                                BteRecipeCategory category,
                                NonNullList<Ingredient> ingredients, ItemStack result,
                                Object... extra);
    }
}

