package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.*;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
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
    public static final RegistryObject<RecipeType<WarlockPotionRecipe>> WARLOCK_POTION_RECIPE = RECIPE_TYPES.register("warlock_potion", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });

    public static final RegistryObject<RecipeType<DruidRecipe>> DRUID_RECIPE_TYPE = RECIPE_TYPES.register("druid_recipe_type", ()-> new RecipeType<>() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    });


    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
    }
}
