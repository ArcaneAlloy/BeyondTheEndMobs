package fr.shoqapik.btemobs.integration;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.integration.druid_create_jei.DruidCraftCategory;
import fr.shoqapik.btemobs.integration.warlock_enchant_jei.WarlockEnchantCategory;
import fr.shoqapik.btemobs.integration.warlock_potion_jei.WarlockPotionCategory;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static RecipeType<DruidRecipe> DRUID_CRAFT_TYPE =
            new RecipeType<>(DruidCraftCategory.UID, DruidRecipe.class);
    public static RecipeType<WarlockPotionRecipe> WARLOCK_POTION_TYPE =
            new RecipeType<>(WarlockPotionCategory.UID, WarlockPotionRecipe.class);

    public static RecipeType<WarlockRecipe> WARLOCK_ENCHANT_TYPE =
            new RecipeType<>(WarlockEnchantCategory.UID, WarlockRecipe.class);


    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BteMobsMod.MODID,"jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration){
        registration.addRecipeCategories(new DruidCraftCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WarlockPotionCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WarlockEnchantCategory(registration.getJeiHelpers().getGuiHelper()));

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration){
        RecipeManager rm = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        List<DruidRecipe> recipeDruid = rm.getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
        List<WarlockRecipe> recipeWarlock = rm.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());
        List<WarlockPotionRecipe> recipeWarlockPotion = rm.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get());

        if (!recipeDruid.isEmpty()) {
            registration.addRecipes(JEIPlugin.DRUID_CRAFT_TYPE, recipeDruid);
        }
        if (!recipeWarlockPotion.isEmpty()){
            List<WarlockPotionRecipe> potions = new ArrayList<>();
            for (WarlockPotionRecipe recipe : recipeWarlockPotion){
                WarlockPotionRecipe recipe1 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe1.type = Items.SPLASH_POTION;
                WarlockPotionRecipe recipe2 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe2.type = Items.LINGERING_POTION;
                WarlockPotionRecipe recipe3 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe3.upgrade = Items.GLOWSTONE_DUST;
                WarlockPotionRecipe recipe4 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe4.upgrade = Items.REDSTONE;

                WarlockPotionRecipe recipe13 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe13.type = Items.SPLASH_POTION;
                recipe13.upgrade = Items.GLOWSTONE_DUST;
                WarlockPotionRecipe recipe14 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe14.type = Items.SPLASH_POTION;
                recipe14.upgrade = Items.REDSTONE;

                WarlockPotionRecipe recipe23 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe23.type = Items.LINGERING_POTION;
                recipe23.upgrade = Items.GLOWSTONE_DUST;
                WarlockPotionRecipe recipe24 = new WarlockPotionRecipe(recipe.getIngredientPrimary(),recipe.getId(),recipe.effect,recipe.getTier());
                recipe24.type = Items.LINGERING_POTION;
                recipe24.upgrade = Items.REDSTONE;
                potions.add(recipe4);
                potions.add(recipe3);
                potions.add(recipe1);
                potions.add(recipe2);
                potions.add(recipe14);
                potions.add(recipe13);
                potions.add(recipe24);
                potions.add(recipe23);
            }
            registration.addRecipes(JEIPlugin.WARLOCK_POTION_TYPE,potions);
            registration.addRecipes(JEIPlugin.WARLOCK_POTION_TYPE, recipeWarlockPotion);
        }
        if(!recipeWarlock.isEmpty()){
            registration.addRecipes(JEIPlugin.WARLOCK_ENCHANT_TYPE, recipeWarlock);
        }

    }
}
