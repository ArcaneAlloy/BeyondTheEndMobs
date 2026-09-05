package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.entity.BteNpcType;

public class UnlockOptionDialogRewardData extends RewardData {
    public String dialogOptionId;
    public BteNpcType npcType;
    public UnlockOptionDialogRewardData(BteNpcType npcType,String dialogOptionId) {
        super(Type.UNLOCK_OPTION_DIALOG, 1);
        this.npcType = npcType;
        this.dialogOptionId = dialogOptionId;
    }

    @Override
    public String getObjectId() {
        return dialogOptionId;
    }
}
