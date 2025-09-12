package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data that will be saved to the world in .nbt form
 * For saving across server restarts.
 * Remember to call markDirty() after setting a value to ensure it saves
 *
 */
public class ServerData extends SavedData {
	private List<UnlockRecipe> recipes;
	public List<UnlockRecipe> getStructureManager() {
		if (this.recipes == null) {
			this.recipes = new ArrayList<>();

			for(WarlockRecipe recipe : BteMobsMod.getServer().getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get())){
				this.recipes.add(new UnlockRecipe(recipe,true));
			}
		}

		return this.recipes;
	}
	public boolean isUnlock(Recipe<?> recipe){
		Optional<UnlockRecipe> recipe1 = getStructureManager().stream().filter(e->e.recipe==recipe).findFirst();

		return recipe1.isPresent() && !recipe1.get().isLock && recipe1.get().wasFound;
	}

	public UnlockRecipe getUnlockRecipe(Recipe<?> recipe){
		Optional<UnlockRecipe> recipe1 = getStructureManager().stream().filter(e->e.recipe==recipe).findFirst();

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
		if(!getStructureManager().isEmpty()){
			for (UnlockRecipe recipe : getStructureManager()){
				listTag.add(recipe.savedData());
			}
		}
		data.put("unlockRecipes",listTag);
		return data;
	}
	public static ServerData load(CompoundTag data) {
		ServerData created = new ServerData();
		List<UnlockRecipe> list = new ArrayList<>();
		if(data.contains("unlockRecipes")){
			CompoundTag tag = data.getCompound("unlockRecipes");
			ListTag tags = data.getList("unlockRecipes",10);
			for(int i = 0; i < tags.size() ; i++){
				CompoundTag nbt = tags.getCompound(i);
				UnlockRecipe recipe = new UnlockRecipe(nbt);
				list.add(recipe);
			}
		}
		created.recipes = list;
		return created;
	}

}
