package fr.shoqapik.btemobs.quest;

public class ItemRewardData extends RewardData {
    public String itemId;
    public ItemRewardData(String itemId,int count) {
        super(Type.ITEM, count);
        this.itemId = itemId;
    }

    @Override
    public String getObjectId() {
        return this.itemId;
    }
}
