package fr.shoqapik.btemobs.quest;

public class ConditionUnlockData {
    public String locationId;
    public Type type;
    public ConditionUnlockData(Type type,String id){
        this.locationId = id;
        this.type = type;
    }

    public String getLocationId() {
        return locationId;
    }

    public enum Type{
        ADVANCEMENT,
        PARENT_QUEST
    }
}
