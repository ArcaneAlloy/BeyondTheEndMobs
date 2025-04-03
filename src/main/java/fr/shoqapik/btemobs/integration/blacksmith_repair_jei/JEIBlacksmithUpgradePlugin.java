package fr.shoqapik.btemobs.integration.blacksmith_repair_jei;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
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
public class JEIBlacksmithUpgradePlugin implements IModPlugin {
    public static RecipeType<BlacksmithUpgradeRecipe> BLACKSMITH_UPGRADE_TYPE =
            new RecipeType<>(BlacksmithUpgradeCategory.UID, BlacksmithUpgradeRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BteMobsMod.MODID,"jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration){

        System.out.println("Registrando categorías en JEI... ----------------------------------------------------------------------------------------");

        registration.addRecipeCategories(new BlacksmithUpgradeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration){
        RecipeManager rm = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        List<BlacksmithUpgradeRecipe> recipeBlacksmith = rm.getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get());

        System.out.println("----------------------------------------------------------------------------------------------------------------------------");

        System.out.println("Registrando " + recipeBlacksmith.size() + " recetas en JEI...");

        if (!recipeBlacksmith.isEmpty()) {
            registration.addRecipes(JEIBlacksmithUpgradePlugin.BLACKSMITH_UPGRADE_TYPE, recipeBlacksmith);
            System.out.println("Recetas registradas correctamente en JEI.");
        } else {
            System.out.println("No se encontraron recetas para la categoría de mejora de herrero.");
        }

        registration.addRecipes(BLACKSMITH_UPGRADE_TYPE, recipeBlacksmith);
    }
}
