package fr.shoqapik.btemobs.quest;

public class RewardData {
    public String itemId;
    public int count;
    public Type type;
    public RewardData(Type type,String id,int count){

        this.type = type;
        this.itemId = id;
        this.count = count;
    }
    public enum Type{
        ITEM,
        UNLOCK_RECIPE,
        UNLOCK_OPTION_DIALOG,
        UNLOCK_SYSTEM,
        UNLOCK_ABILITY,
        UNLOCK_ZONE,
    }
}
