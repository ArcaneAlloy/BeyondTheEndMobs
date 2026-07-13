package fr.shoqapik.btemobs.capability;

import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.nbt.CompoundTag;

public class StatRewardData {
    public String id;
    public int count = 0;
    public int maxCount = 0;
    public boolean complete = false;
    public RewardData.Type rewardType;
    public StatRewardData(String id,int count,int maxCount,boolean complete , RewardData.Type rewardType){
        this.id = id;
        this.count = count;
        this.maxCount = maxCount;
        this.complete = complete;
        this.rewardType = rewardType;
    }
    public StatRewardData(CompoundTag tag){
        this.id = tag.getString("id");
        this.count = tag.getInt("count");
        this.maxCount = tag.getInt("maxCount");
        this.complete = tag.getBoolean("complete");
        this.rewardType = RewardData.Type.valueOf(tag.getString("type"));
    }
    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("id",id);
        tag.putInt("count",count);
        tag.putInt("maxCount",maxCount);
        tag.putBoolean("complete",complete);
        tag.putString("type",this.rewardType.name());
        return tag;
    }
    public int getCount() {
        return count;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public boolean isComplete() {
        return complete;
    }
}
