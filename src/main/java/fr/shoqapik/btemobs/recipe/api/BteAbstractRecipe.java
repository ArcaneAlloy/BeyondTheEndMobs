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
import net.minecraftforge.common.crafting.CraftingHelper;

public abstract class BteAbstractRecipe implements Recipe<BteAbstractCraftContainer> {

    private final ResourceLocation resourceLocation;
    protected final BteRecipeCategory category;
    protected final NonNullList<Ingredient> ingredients;
    protected final ItemStack result;

    public BteAbstractRecipe(ResourceLocation resourceLocation, BteRecipeCategory category, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.resourceLocation = resourceLocation;
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public ResourceLocation getId() {
        return this.resourceLocation;
    }

    public BteRecipeCategory getCategory() {
        return this.category;
    }

    @Override
    public ItemStack getResultItem() {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public boolean matches(BteAbstractCraftContainer inventory, Level level) {
        for(Ingredient ingredient : getIngredients()) {
            boolean hasEnough = false;

            for(ItemStack itemStack : ingredient.getItems()) {
                if(inventory.countItem(itemStack.getItem()) == itemStack.getCount()) {
                    hasEnough = true;
                    break;
                }
            }

            if(!hasEnough) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(BteAbstractCraftContainer inventory) {
        return this.getResultItem().copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= this.ingredients.size();
    }

    public boolean hasItems(Player player) {
        for(Ingredient ingredient : getIngredients()) {
            boolean hasEnough = false;

            for(ItemStack itemStack : ingredient.getItems()) {
                if(player.getInventory().countItem(itemStack.getItem()) >= itemStack.getCount()) {
                    hasEnough = true;
                    break;
                }
            }

            if(!hasEnough) return false;
        }
        return true;
    }

    public static abstract class AbstractSerializer<T extends BteAbstractRecipe> implements RecipeSerializer<T> {
        @Override
        public T fromJson(ResourceLocation recipeId, JsonObject json) {
            return fromJson(recipeId, json, new Object[0]);
        }

        protected T fromJson(ResourceLocation recipeId, JsonObject json, Object... objects) {
            BteRecipeCategory category = BteRecipeCategory.valueOf(json.get("category").getAsString());
            NonNullList<Ingredient> nonnulllist = itemsFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for bte recipe");
            } else if (nonnulllist.size() > 6) {
                throw new JsonParseException("Too many ingredients for bte recipe. The maximum is 6.");
            } else {
                ItemStack itemstack = hasResultItem() ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true) : ItemStack.EMPTY;
                return of(recipeId, category, nonnulllist, itemstack, objects);
            }
        }

        public static NonNullList<Ingredient> itemsFromJson(JsonArray pIngredientArray) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for(int i = 0; i < pIngredientArray.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(pIngredientArray.get(i));

                if (!ingredient.isEmpty()) {
                    nonnulllist.add(ingredient);
                }
            }

            return nonnulllist;
        }

        @Override
        public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer) {
            return fromNetwork(recipeId, pBuffer, new Object[0]);
        }

        protected T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer, Object... objects) {
            BteRecipeCategory category = BteRecipeCategory.valueOf(pBuffer.readUtf());

            int i = pBuffer.readVarInt();
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

            for(int j = 0; j < nonnulllist.size(); ++j) {
                nonnulllist.set(j, Ingredient.fromNetwork(pBuffer));
            }

            ItemStack itemstack = hasResultItem() ? pBuffer.readItem() : ItemStack.EMPTY;
            return of(recipeId, category, nonnulllist, itemstack);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, T recipe) {
            pBuffer.writeUtf(recipe.getCategory().name());
            pBuffer.writeVarInt(recipe.ingredients.size());

            for(Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(pBuffer);
            }

            if(hasResultItem()) pBuffer.writeItem(recipe.result);
        }

        public abstract boolean hasResultItem();

        protected abstract T of(ResourceLocation resourceLocation, BteRecipeCategory category, NonNullList<Ingredient> ingredients, ItemStack result, Object... objects);
    }
}
