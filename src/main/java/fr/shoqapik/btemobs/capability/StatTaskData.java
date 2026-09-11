package fr.shoqapik.btemobs.capability;

import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.nbt.CompoundTag;

public class StatTaskData {
    public String id;
    public int count = 0;
    public int maxCount = 0;
    public boolean complete = false;
    public TaskData.Type taskType;
    public String description;
    public StatTaskData(String id,int count,int maxCount,boolean complete,String description , TaskData.Type taskType){
        this.id = id;
        this.count = count;
        this.maxCount = maxCount;
        this.complete = complete;
        this.taskType = taskType;
        this.description = description;
    }
    public StatTaskData(CompoundTag tag){
        this.id = tag.getString("id");
        this.count = tag.getInt("count");
        this.maxCount = tag.getInt("maxCount");
        this.complete = tag.getBoolean("complete");
        this.taskType = TaskData.Type.valueOf(tag.getString("type"));
        this.description = tag.getString("description");
    }
    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("id",id);
        tag.putInt("count",count);
        tag.putInt("maxCount",maxCount);
        tag.putBoolean("complete",complete);
        tag.putString("type",this.taskType.name());
        tag.putString("description",this.description);
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
