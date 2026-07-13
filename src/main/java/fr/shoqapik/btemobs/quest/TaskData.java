package fr.shoqapik.btemobs.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class TaskData {
    public Type type;
    public String description;
    public int count;
    private String entityIdLocation;
    public TaskData(Type type,String description,String location,int count){
        this.type = type;
        this.description = description;
        this.count = count;
        this.entityIdLocation = location;
    }

    public int getCount() {
        return count;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public String getEntityIdLocation() {
        return entityIdLocation;
    }

    public enum Type {
        HUNTER,
        COLLECT,
        EXPLORING,
        BOSS_HUNTER
    }
}
