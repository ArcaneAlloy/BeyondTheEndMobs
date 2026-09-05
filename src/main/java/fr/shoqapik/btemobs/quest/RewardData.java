package fr.shoqapik.btemobs.quest;

public abstract class RewardData {
    public int count;
    public Type type;
    public RewardData(Type type,int count){
        this.type = type;
        this.count = count;
    }

    public abstract String getObjectId();

    public enum Type{
        ITEM,
        UNLOCK_RECIPE,
        UNLOCK_OPTION_DIALOG,
        UNLOCK_ZONE,
        UNLOCK_SYSTEM,
        UNLOCK_ABILITY
    }
}
