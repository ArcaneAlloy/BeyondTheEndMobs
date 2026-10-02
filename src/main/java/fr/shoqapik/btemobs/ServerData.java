package fr.shoqapik.btemobs;


import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.UnlockAction;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.Quest;
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
	private boolean unlockTwilightForestPortal = false;

	// ---- Modo cooperativo: progreso de quests compartido por todos los jugadores del mundo ----
	private Map<BteNpcType, Map<Quest, QuestStateData>> sharedQuests;
	private List<UnlockAction> sharedUnlockActions;
	/** Datos leídos del disco; se interpretan la primera vez que se piden (cuando las quests ya están cargadas). */
	private CompoundTag pendingSharedQuests;
	/** Generación de QuestManager con la que se sincronizó sharedQuests por última vez (no se guarda). */
	public int questGeneration = -1;

	public Map<BteNpcType, Map<Quest, QuestStateData>> getSharedQuests() {
		if (this.sharedQuests == null && this.pendingSharedQuests != null) {
			this.sharedQuests = RecipeCapability.loadQuests(this.pendingSharedQuests);
			this.sharedUnlockActions = RecipeCapability.loadUnlockActions(this.pendingSharedQuests);
			this.pendingSharedQuests = null;
		}
		return this.sharedQuests;
	}

	public List<UnlockAction> getSharedUnlockActions() {
		getSharedQuests();
		return this.sharedUnlockActions;
	}

	public void setSharedQuests(Map<BteNpcType, Map<Quest, QuestStateData>> quests, List<UnlockAction> unlockActions) {
		this.sharedQuests = quests;
		this.sharedUnlockActions = unlockActions;
		this.pendingSharedQuests = null;
		this.questGeneration = -1;
	}

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
	// Se compara por id: tras un /reload los objetos Recipe cambian pero el id se mantiene
	public boolean isUnlock(Recipe<?> recipe){
		UnlockRecipe unlock = getUnlockRecipe(recipe);
		return unlock != null && !unlock.isLock && unlock.wasFound;
	}

	public UnlockRecipe getUnlockRecipe(Recipe<?> recipe){
		for (UnlockRecipe e : getUnlockRecipesForType(recipe.getType())) {
			if (e.recipe != null && e.recipe.getId().equals(recipe.getId())) return e;
		}
		return null;
	}

	/** Como getUnlockRecipe, pero crea la entrada (bloqueada) si la receta es nueva (p. ej. añadida por un datapack). */
	public UnlockRecipe getOrCreateUnlockRecipe(Recipe<?> recipe){
		UnlockRecipe unlock = getUnlockRecipe(recipe);
		if (unlock == null) {
			unlock = new UnlockRecipe(recipe, true);
			getRecipesManager().computeIfAbsent(recipe.getType(), k -> new ArrayList<>()).add(unlock);
		}
		return unlock;
	}

	/** Marca la receta como desbloqueada para todo el mundo. */
	public void markUnlocked(Recipe<?> recipe){
		UnlockRecipe unlock = getOrCreateUnlockRecipe(recipe);
		unlock.setWasFound(true);
		unlock.setIsLock(false);
		setDirty();
	}

	public boolean unlockTwilightForestPortal(){
		return this.unlockTwilightForestPortal;
	}
	public void unlockPortal(){
		this.unlockTwilightForestPortal = true;
		setDirty();
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
				if(key!=null){
					tag.putString("type",ForgeRegistries.RECIPE_TYPES.getKey(key).toString());
					for (UnlockRecipe recipe : list){
						recipes.add(recipe.savedData());
					}
					tag.put("recipes",recipes);
					listTag.add(tag);
				}
			});
		}
		data.putBoolean("unlockTheTwilightForest",this.unlockTwilightForestPortal);
		if (this.sharedQuests != null) {
			data.put("sharedQuests", RecipeCapability.saveQuests(this.sharedQuests, this.sharedUnlockActions));
		} else if (this.pendingSharedQuests != null) {
			data.put("sharedQuests", this.pendingSharedQuests);
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
				if(!ResourceLocation.isValidResourceLocation(nbt.getString("type")))continue;
				RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(nbt.getString("type")));
				if (type == null)
					continue;
				for (int j = 0 ; j<recipeData.size() ; j++){
					CompoundTag tag = recipeData.getCompound(j);
					UnlockRecipe recipe = new UnlockRecipe(tag);
					// Saltar recetas cuyo ID ya no existe en el RecipeManager
					if (recipe.recipe == null) continue;
					list.add(recipe);
				}
				map.put(type,list);
			}
		}
		created.unlockTwilightForestPortal = data.getBoolean("unlockTheTwilightForest");
		if (data.contains("sharedQuests")) {
			created.pendingSharedQuests = data.getCompound("sharedQuests");
		}
		created.recipes = map;
		return created;
	}

}
