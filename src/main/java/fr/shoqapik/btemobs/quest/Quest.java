package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    public ResourceLocation id;
    private final BteNpcType entityType;
    private final String description;
    private final String toolTip;
    private final List<TaskData> tasks;
    private final List<RewardData> rewards;
    private final List<ConditionUnlockData> conditionUnlockData;
    private final  int experience;
    private final Difficult difficult;
    private final LocationDimension dimension;
    private final PriorityQuest priorityQuest;

    public Quest(BteNpcType entityId, String description, String toolTip, List<TaskData> tasks, List<RewardData> rewards,
                 List<ConditionUnlockData> conditionUnlockData, Difficult difficult,
                 LocationDimension dimension,PriorityQuest priorityQuest, int experience){
        this.entityType = entityId;
        this.description = description;
        this.toolTip = toolTip;
        this.tasks = tasks;
        this.rewards = rewards;
        this.conditionUnlockData = conditionUnlockData;
        this.experience = experience;
        this.dimension = dimension;
        this.difficult = difficult;
        this.priorityQuest = priorityQuest;
    }

    public static void encode(Quest quest, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeUtf(quest.id.toString());
        packetBuffer.writeUtf(quest.entityType.name());
        packetBuffer.writeUtf(quest.toolTip);
        packetBuffer.writeUtf(quest.description);
        packetBuffer.writeInt(quest.experience);

        packetBuffer.writeInt(quest.tasks.size());
        for (TaskData task : quest.tasks) {
            packetBuffer.writeUtf(task.type.name());
            packetBuffer.writeUtf(task.description);
            packetBuffer.writeUtf(task.getEntityIdLocation());
            packetBuffer.writeInt(task.count);
        }

        packetBuffer.writeInt(quest.rewards.size());
        for (RewardData rewardData : quest.rewards) {
            packetBuffer.writeUtf(rewardData.type.name());
            packetBuffer.writeUtf(rewardData.getObjectId());
            if (rewardData.type == RewardData.Type.ITEM){
                packetBuffer.writeInt(rewardData.count);
            }else if (rewardData instanceof UnlockRecipeRewardData data){
                packetBuffer.writeUtf(data.recipeType);
            }else if (rewardData instanceof UnlockZoneRewardData){
               // No hace falta por ahora
            }else if (rewardData instanceof UnlockOptionDialogRewardData data){
                packetBuffer.writeUtf(data.npcType.name());
            }
        }

        packetBuffer.writeInt(quest.conditionUnlockData.size());
        for (ConditionUnlockData conditionUnlockData1 : quest.conditionUnlockData) {
            packetBuffer.writeUtf(conditionUnlockData1.locationId);
            packetBuffer.writeUtf(conditionUnlockData1.type.name());
        }

        packetBuffer.writeUtf(quest.difficult.name());
        packetBuffer.writeUtf(quest.dimension.name());
        packetBuffer.writeEnum(quest.priorityQuest);
    }

    public static Quest decode(FriendlyByteBuf packetBuffer) {
        ResourceLocation id = ResourceLocation.tryParse(packetBuffer.readUtf());
        BteNpcType entityId = BteNpcType.valueOf(packetBuffer.readUtf());

        String tooltip = packetBuffer.readUtf();
        String description = packetBuffer.readUtf();
        int xp = packetBuffer.readInt();

        int tasksNumber = packetBuffer.readInt();
        List<TaskData> tasks = new ArrayList<>();
        for (int i = 0; i < tasksNumber; i++) {
            tasks.add(new TaskData(TaskData.Type.valueOf(packetBuffer.readUtf()), packetBuffer.readUtf(), packetBuffer.readUtf(), packetBuffer.readInt()));
        }

        int rewardsNumber = packetBuffer.readInt();
        List<RewardData> rewards = new ArrayList<>();
        for (int i = 0; i < rewardsNumber; i++) {
            RewardData.Type type = RewardData.Type.valueOf(packetBuffer.readUtf());

            String objectId = packetBuffer.readUtf();
            if (type == RewardData.Type.ITEM){
                int count = packetBuffer.readInt();
                rewards.add(new ItemRewardData(objectId,count));
            }else if (type == RewardData.Type.UNLOCK_OPTION_DIALOG){
                BteNpcType bteNpcType = BteNpcType.valueOf(packetBuffer.readUtf());
                rewards.add(new UnlockOptionDialogRewardData(bteNpcType,objectId));
            }else if (type == RewardData.Type.UNLOCK_RECIPE){
                String recipeType = packetBuffer.readUtf();
                rewards.add(new UnlockRecipeRewardData(objectId,recipeType));
            }else if (type == RewardData.Type.UNLOCK_ZONE){
                rewards.add(new UnlockZoneRewardData(objectId));
            }

        }

        int conditionUnlockNumber = packetBuffer.readInt();
        List<ConditionUnlockData> conditionUnlocks = new ArrayList<>();
        for (int i = 0; i < conditionUnlockNumber; i++) {
            String location = packetBuffer.readUtf();
            ConditionUnlockData.Type type = ConditionUnlockData.Type.valueOf(packetBuffer.readUtf());
            conditionUnlocks.add(new ConditionUnlockData(type,location ));
        }
        Difficult difficult1 = Difficult.valueOf(packetBuffer.readUtf());
        LocationDimension dimension1 = LocationDimension.valueOf(packetBuffer.readUtf());
        PriorityQuest priorityQuest1 = packetBuffer.readEnum(PriorityQuest.class);
        Quest quest = new Quest(entityId, description,tooltip , tasks, rewards, conditionUnlocks, difficult1, dimension1,priorityQuest1, xp);
        quest.id = id;
        return quest;
    }

    public String getToolTip() {
        return toolTip;
    }

    public LocationDimension getDimension() {
        return dimension;
    }

    public String getDescription() {
        return description;
    }

    public Difficult getDifficult() {
        return difficult;
    }

    public int getExperience() {
        return experience;
    }

    public BteNpcType getEntityType() {
        return entityType;
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<ConditionUnlockData> getConditionUnlockData() {
        return conditionUnlockData;
    }

    public List<TaskData> getTasks() {
        return tasks;
    }

    public List<RewardData> getRewards() {
        return rewards;
    }

    public PriorityQuest getPriorityQuest() {
        return priorityQuest;
    }

    public enum Difficult {
        HARD(0x4DFF0000 ),
        MEDIUM(0x4DFFFF00),
        EASY(0x4D00FF00);
        public int color;
        Difficult(int color){
            this.color = color;
        }
    }
    public enum LocationDimension{
        OVERWORLD,
        NETHER,
        END,
        TWILIGHT_FOREST
    }
    public enum PriorityQuest{
        MAIN_QUEST,
        SIDE_QUEST;
    }
}