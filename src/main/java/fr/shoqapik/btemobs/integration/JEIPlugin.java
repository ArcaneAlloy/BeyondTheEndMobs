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
import net.minecraft.world.item.crafting.RecipeManager;

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
            registration.addRecipes(JEIPlugin.WARLOCK_POTION_TYPE, recipeWarlockPotion);
        }
        if(!recipeWarlock.isEmpty()){
            registration.addRecipes(JEIPlugin.WARLOCK_ENCHANT_TYPE, recipeWarlock);
        }

    }
}
