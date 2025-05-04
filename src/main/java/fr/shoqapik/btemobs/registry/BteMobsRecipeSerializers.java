package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BteMobsRecipeSerializers {

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BteMobsMod.MODID);

    public static final RegistryObject<RecipeSerializer<BlacksmithRecipe>> BLACKSMITH_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("blacksmith", BlacksmithRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<BlacksmithUpgradeRecipe>> BLACKSMITH_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("blacksmith_upgrade", BlacksmithUpgradeRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<WarlockRecipe>> WARLOCK_SERIALIZER = RECIPE_SERIALIZERS.register("warlock", WarlockRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<ExplorerRecipe>> EXPLORER_RECIPE = RECIPE_SERIALIZERS.register("explorer_recipe", ExplorerRecipe.Serializer::new);


    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
