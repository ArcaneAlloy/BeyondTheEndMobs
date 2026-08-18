package fr.shoqapik.btemobs.capability;


import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.api.RecipePlayer;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.SyncRecipeManager;
import fr.shoqapik.btemobs.quest.*;

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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class RecipeCapability <T extends Recipe<?>> implements RecipePlayer<T> {
    public Player player;
    private Map<RecipeType<T>, List<T>> recipeManager = new HashMap<>();
    public boolean dirty = false;
    private Level level ;
    public Map<BteNpcType,Map<Quest,QuestStateData>> quests=new HashMap<>();

    public static RecipeCapability get(Player player){
        return player.getCapability(BteCapability.RECIPE_CAPABILITY,null).orElse(null);
    }
    public Map<Quest,QuestStateData> getQuestForNpc(BteNpcType npcType){

        return quests.get(npcType);
    }
    public void checkChangedInventory(){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData data : questMap.values()) {

                List<StatTaskData> list = data.statTaskData;
                boolean unlock = data.unlockStates.isEmpty()
                        || data.unlockStates.stream().allMatch(e -> e.unlock);

                int taskComplete = 0;

                if (unlock) {
                    for (StatTaskData task : list) {
                        if (!task.complete) {
                            if (task.taskType == TaskData.Type.COLLECT) {
                                task.count = Math.min(task.maxCount,
                                        player.getInventory().countItem(ForgeRegistries.ITEMS.getValue(new ResourceLocation(task.id))));
                                if (task.count == task.maxCount) {
                                    taskComplete++;
                                }
                            }
                        } else {
                            taskComplete++;
                        }
                    }

                    if (taskComplete == list.size()) {
                        data.isComplete = true;
                        this.dirty = true;
                    }
                }
            }
        }
    }
    public void completeQuest(Quest quest){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {

                QuestStateData data = entry.getValue();

                for (UnlockState state : data.unlockStates) {
                    if (state.type == ConditionUnlockData.Type.PARENT_QUEST
                            && state.id.equals(quest.id.toString())) {
                        state.unlock = true;
                    }
                }
            }
        }
    }

    public void hunterQuestUpdate(LivingDeathEvent event){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {

                if (entry.getValue().isComplete)continue;
                List<StatTaskData> list = entry.getValue().statTaskData;
                boolean unlock = (entry.getValue().unlockStates.isEmpty() || entry.getValue().unlockStates.stream().allMatch(e->e.unlock));
                int taskComplete = 0;
                if (unlock){
                    for (StatTaskData statTaskData : list) {
                        if (!statTaskData.complete){
                            if (statTaskData.id.equals(event.getEntity().getEncodeId())) {

                                statTaskData.count = Math.min(statTaskData.count + 1, statTaskData.maxCount);
                                if (statTaskData.count== statTaskData.maxCount){
                                    statTaskData.complete = true;
                                    taskComplete++;
                                }
                            }
                        }else {
                            taskComplete++;
                        }

                    }
                    if (taskComplete == list.size()){
                        entry.getValue().isComplete = true;
                        this.dirty = true;
                    }
                }

            }
        }
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    public boolean getQuestComplete(Quest quest){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey().id.equals(quest.id)) {
                    return entry.getValue().isComplete;
                }
            }
        }
        return false;
    }

    public boolean getQuestReclaim(Quest quest){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey().id.equals(quest.id)) {
                    return entry.getValue().isReclaim;
                }
            }
        }
        return false;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void tick(Player player) {
        if(!level.isClientSide){
            if (this.quests.size() != QuestManager.getQuests().size()){
                for (Quest quest : QuestManager.getQuests()){
                    if (!this.quests.get(quest.getEntityType()).containsKey(quest)){
                        List<StatRewardData> dataList = new ArrayList<>();
                        List<StatTaskData> taskDataList = new ArrayList<>();
                        List<UnlockState> unlockStates = new ArrayList<>();
                        for (TaskData data:quest.getTasks()){
                            taskDataList.add(new StatTaskData(data.getEntityIdLocation(),0,data.count,false,data.type));
                        }
                        for (RewardData rewardData : quest.getRewards()){
                            dataList.add(new StatRewardData(rewardData.itemId,0,rewardData.count,false,rewardData.type));
                        }
                        for (ConditionUnlockData unlockData : quest.getConditionUnlockData()){
                            unlockStates.add(new UnlockState(unlockData.locationId,unlockData.type,false));
                        }
                        quests.get(quest.getEntityType()).put(quest,new QuestStateData(false,false,dataList,taskDataList,unlockStates));
                    }
                }
                this.dirty = true;
            }
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
            if(!this.recipeManager.get(type).contains(recipe)){
                this.recipeManager.get(type).add(recipe);
            }
        }else {
            this.recipeManager.put(type,List.of(recipe));
        }

        this.dirty = true;
    }


    public void addRecipesForType(RecipeType<T> type, List<T> recipe) {
        if(this.recipeManager.containsKey(type)){
            for(T r : recipe){
                if(!this.recipeManager.get(type).contains(r)){
                    this.recipeManager.get(type).add(r);
                }
            }

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
            this.initQuest();

            this.dirty = true;
        }
    }

    public void completeQuest(ResourceLocation id){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey().id.equals(id)) {
                    entry.getValue().isReclaim = true;
                }
            }
        }
        dirty = true;
    }

    public void initQuest(){
        Map<BteNpcType,Map<Quest,QuestStateData>> map = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()){
            map.put(type,new HashMap<>());
        }
        for (Map.Entry<BteNpcType,List<Quest>> entry : QuestManager.getQuestsForType().entrySet()){
            for (Quest quest : entry.getValue()){
                List<StatRewardData> dataList = new ArrayList<>();
                List<StatTaskData> taskDataList = new ArrayList<>();
                List<UnlockState> unlockStates = new ArrayList<>();
                for (TaskData data:quest.getTasks()){
                    taskDataList.add(new StatTaskData(data.getEntityIdLocation(),0,data.count,false,data.type));
                }
                for (RewardData rewardData : quest.getRewards()){
                    dataList.add(new StatRewardData(rewardData.itemId,0,rewardData.count,false,rewardData.type));
                }
                for (ConditionUnlockData unlockData : quest.getConditionUnlockData()){
                    unlockStates.add(new UnlockState(unlockData.locationId,unlockData.type,false));
                }
                map.get(entry.getKey()).put(quest,new QuestStateData(false,false,dataList,taskDataList,unlockStates));
            }
        }
        this.quests = map;
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
        if (!quests.isEmpty()){
            ListTag list = new ListTag();
            quests.entrySet().forEach((entry)->{
                if(entry.getKey()==null){
                    return;
                }
                CompoundTag tag = new CompoundTag();
                tag.putString("type",entry.getKey().name());
                ListTag tags = new ListTag();
                entry.getValue().entrySet().forEach(entry1->{
                    CompoundTag tag1 = new CompoundTag();
                    tag1.putString("id",entry1.getKey().id.toString());
                    tag1.put("data",entry1.getValue().save());
                    tags.add(tag1);
                });
                tag.put("list",tags);
                list.add(tag);
            });
            nbt.put("quests",list);
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
        Map<BteNpcType,Map<Quest,QuestStateData>> map1 = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()){
            map1.put(type,new HashMap<>());
        }
        if (nbt.contains("quests")){
            ListTag listTag = nbt.getList("quests",10);

            for (int i = 0 ; i < listTag.size() ; i++){
                CompoundTag nbt1 = listTag.getCompound(i);
                BteNpcType type = BteNpcType.valueOf(nbt1.getString("type"));
                if (nbt1.contains("list")){
                    ListTag listTag1 = nbt1.getList("list",10);
                    for (int j = 0;j < listTag1.size() ; j++){
                        CompoundTag nbt2 = listTag1.getCompound(j);
                        Quest quest = QuestManager.getQuest(nbt2.getString("id"));
                        if (quest == null) continue; // ID de quest guardado que ya no existe (renombrada/eliminada); se ignora en vez de corromper el mapa
                        map1.get(type).put(quest,new QuestStateData(nbt2.getCompound("data")));
                    }
                }
            }

        }

        this.recipeManager = map;
        this.quests = map1;
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
