package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Data that will be saved to the world in .nbt form
 * For saving across server restarts.
 * Remember to call markDirty() after setting a value to ensure it saves
 *
 */
public class ServerData extends SavedData {
	private Map<RecipeType<?>,List<UnlockRecipe>> recipes;
	public Map<RecipeType<?>,List<UnlockRecipe>> getRecipesManager() {
		if (this.recipes == null) {
			Collection<RecipeType<?>> recipeTypes = ForgeRegistries.RECIPE_TYPES.getValues();
			Map<RecipeType<?>,List<UnlockRecipe>> map = new HashMap<>();
			for (RecipeType<?> type : recipeTypes){
				List<UnlockRecipe> recipes = new ArrayList<>();
				for(Recipe<?> recipe : getRecipes(type)){
					recipes.add(new UnlockRecipe(recipe,true));
				}
				map.put(type,recipes);
			}
			this.recipes = map;
		}

		return this.recipes;
	}
	@SuppressWarnings("unchecked")
	public static List<Recipe<?>> getRecipes(RecipeType<?> type) {
		return (List<Recipe<?>>) (List<?>)
				BteMobsMod.getServer().getRecipeManager().getAllRecipesFor((RecipeType) type);
	}
	public List<UnlockRecipe> getUnlockRecipesForType(RecipeType<?> type){
		return getRecipesManager().getOrDefault(type,new ArrayList<>());
	}
	public boolean isUnlock(Recipe<?> recipe){
		Optional<UnlockRecipe> recipe1 = getUnlockRecipesForType(recipe.getType()).stream().filter(e->e.recipe==recipe).findFirst();

		return recipe1.isPresent() && !recipe1.get().isLock && recipe1.get().wasFound;
	}

	public UnlockRecipe getUnlockRecipe(Recipe<?> recipe){
		Optional<UnlockRecipe> recipe1 = getUnlockRecipesForType(recipe.getType()).stream().filter(e->e.recipe==recipe).findFirst();

		return recipe1.orElse(null);
	}

	public static ServerData get() {
		DimensionDataStorage manager = BteMobsMod.getServer().getLevel(Level.OVERWORLD)
				.getDataStorage();

		ServerData state = manager.computeIfAbsent(
				ServerData::load,
				ServerData::new,
				BteMobsMod.MODID
		);
		state.setDirty();

		return state;
	}

	@Override
	public CompoundTag save(CompoundTag data) {
		ListTag listTag=new ListTag();
		if(!getRecipesManager().isEmpty()){
			getRecipesManager().forEach((key,list)->{
				CompoundTag tag = new CompoundTag();
				ListTag recipes = new ListTag();
				tag.putString("type",key.toString());
				for (UnlockRecipe recipe : list){
					recipes.add(recipe.savedData());
				}
				tag.put("recipes",recipes);
				listTag.add(tag);
			});
		}
		data.put("unlockRecipes",listTag);
		return data;
	}
	public static ServerData load(CompoundTag data) {
		ServerData created = new ServerData();
		Map<RecipeType<?>,List<UnlockRecipe>> map = new HashMap<>();

		if(data.contains("unlockRecipes")){
			ListTag tags = data.getList("unlockRecipes",10);
			for(int i = 0; i < tags.size() ; i++){
				List<UnlockRecipe> list = new ArrayList<>();
				CompoundTag nbt = tags.getCompound(i);
				ListTag recipeData = nbt.getList("recipes",10);
				RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(nbt.getString("type")));

				for (int j = 0 ; j<recipeData.size() ; j++){
					CompoundTag tag = recipeData.getCompound(j);
					UnlockRecipe recipe = new UnlockRecipe(tag);
					list.add(recipe);
				}
				map.put(type,list);
			}
		}
		created.recipes = map;
		return created;
	}

}
