package fr.shoqapik.btemobs.capability;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.api.RecipePlayer;
import fr.shoqapik.btemobs.packets.SyncRecipeManager;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class RecipeCapability <T extends Recipe<?>> implements RecipePlayer<T> {
    public Player player;
    private Map<RecipeType<T>, List<T>> recipeManager = new HashMap<>();
    private boolean dirty = false;
    private Level level ;
    public static RecipeCapability get(Player player){
        return player.getCapability(BteCapability.RECIPE_CAPABILITY,null).orElse(null);
    }
    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void tick(Player player) {
        if(!level.isClientSide){
            if(this.dirty){
                BteMobsMod.sendToClient(new SyncRecipeManager(player.getId(),this.serializeNBT(),false), (ServerPlayer) player);
                this.dirty = false;
            }
        }
    }
    public void copyFrom(RecipeCapability cap){
        this.recipeManager = cap.recipeManager;
        this.dirty = true;
    }
    @Override
    public void onJoinGame(Player player, EntityJoinLevelEvent event) {

    }

    @Override
    public void setRecipesForType(RecipeType<T> type, List<T> recipes) {
        Map<RecipeType<T>,List<T>> map = new HashMap<>(this.getRecipeManager());
        map.put(type,recipes);
        this.recipeManager = map;
    }

    @Override
    public void addRecipeForType(RecipeType<T> type, T recipe) {
        if(this.recipeManager.containsKey(type)){
            this.recipeManager.get(type).add(recipe);
        }else {
            this.recipeManager.put(type,List.of(recipe));
        }

        this.dirty = true;
    }

    public void addRecipesForType(RecipeType<T> type, List<T> recipe) {
        if(this.recipeManager.containsKey(type)){
            this.recipeManager.get(type).addAll(recipe);
        }else {
            this.recipeManager.put(type,recipe);
        }
        this.dirty = true;
    }
    public void setRecipeManager(Map<RecipeType<T>,List<T>> map){
        this.recipeManager = map;
    }
    @Override
    public Map<RecipeType<T>, List<T>> getRecipeManager() {
        return this.recipeManager;
    }

    @Override
    public List<T> getRecipesForType(RecipeType<T> type) {
        return getRecipeManager().get(type);
    }

    @Override
    public void init(Player player,Level level) {
        this.setPlayer(player);
        this.level = level;
        if(!this.level.isClientSide){
            this.dirty = true;
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if(!getRecipeManager().isEmpty()){
            ListTag list = new ListTag();
            getRecipeManager().forEach((key,value)->{
                CompoundTag tag = new CompoundTag();
                if (ForgeRegistries.RECIPE_TYPES.getKey(key)!=null){
                    tag.putString("type",ForgeRegistries.RECIPE_TYPES.getKey(key).toString());
                    ListTag list1 = new ListTag();
                    value.forEach(recipe -> {
                        CompoundTag tag1 = new CompoundTag();
                        tag1.putString("recipe",recipe.getId().toString());
                        list1.add(tag1);
                    });
                    tag.put("recipes",list1);
                    list.add(tag);
                }
            });
            nbt.put("manager",list);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        Map<RecipeType<T>,List<T>> map = new HashMap<>();
        if(nbt.contains("manager")){
            ListTag list = nbt.getList("manager",10);
            for (int i = 0 ; i < list.size() ; i ++){
                CompoundTag tag = list.getCompound(i);
                if (!ResourceLocation.isValidResourceLocation(tag.getString("type")))continue;
                RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(tag.getString("type")));
                if(type==null)continue;
                List<T> recipes = new ArrayList<>();
                if(tag.contains("recipes")){
                    ListTag list1 = tag.getList("recipes",10);
                    for (int j = 0 ; j < list1.size() ; j++){
                        CompoundTag tag1 = list1.getCompound(j);
                        Optional<? extends T> recipe = (Optional<? extends T>) this.level.getRecipeManager().byKey(new ResourceLocation(tag1.getString("recipe")));
                        recipe.ifPresent(recipes::add);
                    }
                }
                map.put((RecipeType<T>) type,recipes);
            }
        }
        this.recipeManager = map;
        this.dirty = true;
    }

    public static class RecipeProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final LazyOptional<RecipePlayer> instance = LazyOptional.of(RecipeCapability::new);

        @NonNull
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BteCapability.RECIPE_CAPABILITY.orEmpty(cap,instance.cast());
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) instance.orElseThrow(NullPointerException::new).serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.orElseThrow(NullPointerException::new).deserializeNBT(nbt);
        }
    }
}
