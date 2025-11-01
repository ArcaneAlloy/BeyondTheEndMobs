package fr.shoqapik.btemobs.integration.explorer_craft_jei;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
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
public class JEIExplorerCraftPlugin implements IModPlugin {
    public static RecipeType<ExplorerRecipe> EXPLORER_CRAFT_TYPE =
            new RecipeType<>(ExplorerCraftCategory.UID, ExplorerRecipe.class);



    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BteMobsMod.MODID,"jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration){

        System.out.println("Registrando categorías en JEI... ----------------------------------------------------------------------------------------");

        registration.addRecipeCategories(new ExplorerCraftCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration){
        RecipeManager rm = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        List<ExplorerRecipe> recipeExplorer = rm.getAllRecipesFor(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get());

        System.out.println("----------------------------------------------------------------------------------------------------------------------------");

        System.out.println("Registrando " + recipeExplorer.size() + " recetas en JEI...");

        if (!recipeExplorer.isEmpty()) {
            registration.addRecipes(JEIExplorerCraftPlugin.EXPLORER_CRAFT_TYPE, recipeExplorer);
            System.out.println("Recetas registradas correctamente en JEI.");
        } else {
            System.out.println("No se encontraron recetas para la categoría de mejora de herrero.");
        }

        registration.addRecipes(EXPLORER_CRAFT_TYPE, recipeExplorer);
    }
}
