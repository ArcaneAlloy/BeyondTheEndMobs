package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BteMobsRecipeTypes {

    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, BteMobsMod.MODID);

    public static final RegistryObject<RecipeType<BlacksmithRecipe>> BLACKSMITH_RECIPE = RECIPE_TYPES.register("blacksmith", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });
    public static final RegistryObject<RecipeType<BlacksmithUpgradeRecipe>> BLACKSMITH_UPGRADE_RECIPE = RECIPE_TYPES.register("blacksmith_upgrade", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });

    public static final RegistryObject<RecipeType<WarlockRecipe>> WARLOCK_RECIPE = RECIPE_TYPES.register("warlock", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });

    public static final RegistryObject<RecipeType<ExplorerRecipe>> EXPLORER_RECIPE_TYPE = RECIPE_TYPES.register("explorer_recipe_type", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });


    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
    }
}
