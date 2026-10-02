package fr.shoqapik.btemobs.capability;

import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashSet;
import java.util.Set;

public class StatTaskData {
    public String id;
    public int count = 0;
    public int maxCount = 0;
    public boolean complete = false;
    public TaskData.Type taskType;
    public String description;
    /** Ids ya contados (tareas CRAFT_UNIQUE). */
    public Set<String> seen = new HashSet<>();
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
        ListTag seenTag = tag.getList("seen", 8);
        for (int i = 0; i < seenTag.size(); i++) this.seen.add(seenTag.getString(i));
    }
    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("id",id);
        tag.putInt("count",count);
        tag.putInt("maxCount",maxCount);
        tag.putBoolean("complete",complete);
        tag.putString("type",this.taskType.name());
        tag.putString("description",this.description);
        if (!this.seen.isEmpty()) {
            ListTag seenTag = new ListTag();
            for (String s : this.seen) seenTag.add(StringTag.valueOf(s));
            tag.put("seen", seenTag);
        }
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
