package fr.shoqapik.btemobs.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class QuestStateData {
    public boolean isComplete = false;
    public boolean isReclaim = false;
    public List<StatRewardData> statRewardData;
    public List<StatTaskData> statTaskData;
    public List<UnlockState> unlockStates;
    public QuestStateData(boolean isComplete,boolean isReclaim,List<StatRewardData> data,List<StatTaskData> taskData,List<UnlockState> unlockStates){
        this.isReclaim = isReclaim;
        this.isComplete = isComplete;
        this.statRewardData = data;
        this.statTaskData = taskData;
        this.unlockStates = unlockStates;
    }

    public QuestStateData(CompoundTag tag){
        this.isReclaim = tag.getBoolean("isReclaim");
        this.isComplete = tag.getBoolean("complete");
        List<StatRewardData> statRewardData1 = new ArrayList<>();
        if (tag.contains("statReward")){
            ListTag tags = tag.getList("statReward",10);
            for (int i = 0 ; i < tags.size() ; i ++){
                CompoundTag nbt = tags.getCompound(i);
                statRewardData1.add(new StatRewardData(nbt));
            }
        }
        List<StatTaskData> statTaskData1 = new ArrayList<>();
        if (tag.contains("statTask")){
            ListTag tags = tag.getList("statTask",10);
            for (int i = 0 ; i < tags.size() ; i ++){
                CompoundTag nbt = tags.getCompound(i);
                statTaskData1.add(new StatTaskData(nbt));
            }
        }
        List<UnlockState> unlockStates1 = new ArrayList<>();
        if (tag.contains("unlockState")){
            ListTag tags = tag.getList("unlockState",10);
            for (int i = 0 ; i < tags.size() ; i++){
                CompoundTag nbt = tags.getCompound(i);
                unlockStates1.add(new UnlockState(nbt));
            }
        }

        this.statRewardData = statRewardData1;
        this.statTaskData = statTaskData1;
        this.unlockStates = unlockStates1;
    }

    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isReclaim",this.isReclaim);
        tag.putBoolean("complete",this.isComplete);
        ListTag tags = new ListTag();
        for (StatRewardData data : statRewardData){
            tags.add(data.save());
        }
        ListTag tags1 = new ListTag();
        for (StatTaskData data : statTaskData){
            tags1.add(data.save());
        }
        ListTag tags2 = new ListTag();
        for (UnlockState unlockState : unlockStates){
            tags2.add(unlockState.save());
        }
        tag.put("statReward",tags);
        tag.put("statTask",tags1);
        tag.put("unlockState",tags2);
        return tag;
    }
}
