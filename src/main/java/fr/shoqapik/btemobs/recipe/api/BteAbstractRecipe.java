package fr.shoqapik.btemobs.recipe.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import fr.shoqapik.btemobs.BteMobsMod;
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
import java.util.Locale;

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
}

