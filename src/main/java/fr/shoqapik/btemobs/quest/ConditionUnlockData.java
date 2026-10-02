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
        PARENT_QUEST,
        /** Portal del Nether del Forgotten Realm abierto (mecánica de enders_journey: se abre al conseguir 8 Ender Eyes). */
        NETHER_PORTAL,
        /** Portal del End del Forgotten Realm abierto (mecánica de enders_journey: se abre al conseguir 16 Ender Eyes). */
        END_PORTAL
    }
}
