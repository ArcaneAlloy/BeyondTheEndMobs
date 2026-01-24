package fr.shoqapik.btemobs.client;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.model.OrbModel;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onRegisterRecipeBookCategory(RegisterRecipeBookCategoriesEvent event) {
        //registerBookCategories(RecipeBookType.create("BLACKSMITH"),BteNpcType.BLACKSMITH, List.of(), event);
        //registerRecipeCategoryLookups(BteNpcType.BLACKSMITH, List.of(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get(), BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get()), event);
    }
    @SubscribeEvent
    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OrbModel.LAYER_LOCATION,OrbModel::createBodyLayer);

    }
    private static void registerBookCategories(RecipeBookType type,BteNpcType npcType, List<RecipeBookCategories> vanillaCategories, RegisterRecipeBookCategoriesEvent event) {
        List<RecipeBookCategories> recipeBookCategories = new ArrayList<>();
        for(BteRecipeCategory category : BteRecipeCategory.values(npcType)) {
            recipeBookCategories.add(RecipeBookCategories.create(npcType.name() + "_" + category.name(), category.item));
        }
        event.registerBookCategories(type, recipeBookCategories);

        recipeBookCategories.addAll(vanillaCategories);
        if(BteRecipeCategory.ALL.getVanillaCategory(npcType)!=null){
            recipeBookCategories.remove(BteRecipeCategory.ALL.getVanillaCategory(npcType));
            event.registerAggregateCategory(BteRecipeCategory.ALL.getVanillaCategory(npcType), recipeBookCategories);

        }
    }

    private static void registerRecipeCategoryLookups(BteNpcType npcType, List<RecipeType<?>> recipeTypes, RegisterRecipeBookCategoriesEvent event) {
        for (RecipeType<?> recipeType : recipeTypes) {
            event.registerRecipeCategoryFinder(recipeType, (recipe) -> {
                if(recipe instanceof BteAbstractRecipe) {
                    return RecipeBookCategories.valueOf(npcType.name() + "_" + ((BteAbstractRecipe)recipe).getCategory().name());
                }

                return null;
            });
        }
    }
}
