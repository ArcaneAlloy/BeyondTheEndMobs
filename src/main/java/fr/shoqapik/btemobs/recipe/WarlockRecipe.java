package fr.shoqapik.btemobs.recipe;

import com.google.gson.JsonObject;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class WarlockRecipe extends BteAbstractRecipe {

    protected final Enchantment enchantment;
    protected final int level;
    protected final int experience;

    public WarlockRecipe(ResourceLocation resourceLocation, BteRecipeCategory category, ResourceLocation enchantment, int level, int experience, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(resourceLocation, category, ingredients, result);
        this.enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantment);
        if(this.enchantment == null) throw new IllegalArgumentException("Enchantment '%s' not found".formatted(enchantment));
        this.level = level;
        this.experience = experience;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return this.experience;
    }

    @Override
    public ItemStack getResultItem() {
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        result.enchant(enchantment, level);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return new Serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.WARLOCK_RECIPE.get();
    }

    public static class Serializer extends AbstractSerializer<WarlockRecipe> {
        @Override
        public WarlockRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ResourceLocation enchantment = new ResourceLocation(json.get("enchantment").getAsString());
            int level = json.get("level").getAsInt();
            int experience = json.get("experience").getAsInt();
            return fromJson(recipeId, json, enchantment, level, experience);
        }

        @Override
        public WarlockRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer) {
            ResourceLocation enchantment = pBuffer.readResourceLocation();
            int level = pBuffer.readInt();
            int experience = pBuffer.readInt();
            return fromNetwork(recipeId, pBuffer, enchantment, level, experience);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, WarlockRecipe recipe) {
            pBuffer.writeResourceLocation(ForgeRegistries.ENCHANTMENTS.getKey(recipe.enchantment));
            pBuffer.writeInt(recipe.level);
            pBuffer.writeInt(recipe.experience);
            super.toNetwork(pBuffer, recipe);
        }

        @Override
        public boolean hasResultItem() {
            return false;
        }

        @Override
        protected WarlockRecipe of(ResourceLocation resourceLocation, BteRecipeCategory category, NonNullList<Ingredient> ingredients, ItemStack result, Object... objects) {
            return new WarlockRecipe(resourceLocation, category, (ResourceLocation)objects[0], (int)objects[1], (int)objects[2], ingredients, result);
        }
    }
}
