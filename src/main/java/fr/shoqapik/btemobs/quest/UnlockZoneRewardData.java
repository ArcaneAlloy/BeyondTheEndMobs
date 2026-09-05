package fr.shoqapik.btemobs.quest;

public class UnlockZoneRewardData extends RewardData{
    public String zoneId;

    public UnlockZoneRewardData(String zoneId) {
        super(Type.UNLOCK_ZONE, 1);
        this.zoneId=zoneId;
    }

    @Override
    public String getObjectId() {
        return this.zoneId;
    }
}
