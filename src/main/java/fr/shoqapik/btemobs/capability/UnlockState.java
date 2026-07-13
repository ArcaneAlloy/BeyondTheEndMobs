package fr.shoqapik.btemobs.capability;

import fr.shoqapik.btemobs.quest.ConditionUnlockData;
import net.minecraft.nbt.CompoundTag;

public class UnlockState {
    public ConditionUnlockData.Type type;
    public String id;
    public boolean unlock;
    public UnlockState(String id , ConditionUnlockData.Type type,boolean unlock){
        this.id = id;
        this.type = type;
        this.unlock = unlock;
    }
    public UnlockState(CompoundTag tag){
        this.id = tag.getString("id");
        this.type = ConditionUnlockData.Type.valueOf(tag.getString("type"));
        this.unlock = tag.getBoolean("unlock");
    }

    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("id",this.id);
        tag.putString("type",this.type.name());
        tag.putBoolean("unlock",this.unlock);
        return tag;
    }
}
