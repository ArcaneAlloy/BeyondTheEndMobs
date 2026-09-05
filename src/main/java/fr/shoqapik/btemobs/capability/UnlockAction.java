package fr.shoqapik.btemobs.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class UnlockAction implements Unlockeable{
    public String action;
    public List<UnlockState> unlockStates;
    public UnlockAction(String action,List<UnlockState> unlockStates){
        this.action = action;
        this.unlockStates = unlockStates;
    }
    public UnlockAction(CompoundTag tag){
        this.action = tag.getString("action");
        List<UnlockState> unlockStates1 = new ArrayList<>();
        if (tag.contains("unlockState")){
            ListTag tags = tag.getList("unlockState",10);
            for (int i = 0 ; i < tags.size() ; i++){
                CompoundTag nbt = tags.getCompound(i);
                unlockStates1.add(new UnlockState(nbt));
            }
        }
        this.unlockStates = unlockStates1;
    }
    @Override
    public List<UnlockState> getUnlockData() {
        return this.unlockStates;
    }

    public CompoundTag save(){
        CompoundTag tag = new CompoundTag();
        tag.putString("action",this.action);
        ListTag tags2 = new ListTag();
        for (UnlockState unlockState : unlockStates){
            tags2.add(unlockState.save());
        }
        tag.put("unlockState",tags2);
        return tag;
    }
}
