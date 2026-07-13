package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.UnlockState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    public ResourceLocation id;
    private final String entityId;
    private final String description;
    private final List<TaskData> tasks;
    private final List<RewardData> rewards;
    private final List<ConditionUnlockData> conditionUnlockData;
    private final  int experience;
    private final Difficult difficult;
    private final LocationDimension dimension;

    public Quest(String entityId,String description, List<TaskData> tasks, List<RewardData> rewards,
                 List<ConditionUnlockData> conditionUnlockData, Difficult difficult,
                 LocationDimension dimension, int experience){
        this.entityId = entityId;
        this.description = description;
        this.tasks = tasks;
        this.rewards = rewards;
        this.conditionUnlockData = conditionUnlockData;
        this.experience = experience;
        this.dimension = dimension;
        this.difficult = difficult;
    }

    public static void encode(Quest quest, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeUtf(quest.id.toString());
        packetBuffer.writeUtf(quest.entityId);
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
            if (rewardData.type == RewardData.Type.ITEM){
                packetBuffer.writeUtf(rewardData.itemId);
                packetBuffer.writeInt(rewardData.count);
            }else if (rewardData instanceof UnlockRecipeRewardData data){
                BteMobsMod.LOGGER.info("info");
                packetBuffer.writeUtf(data.itemId);
                packetBuffer.writeUtf(data.recipeType);
            }
        }

        packetBuffer.writeInt(quest.conditionUnlockData.size());
        for (ConditionUnlockData conditionUnlockData1 : quest.conditionUnlockData) {
            packetBuffer.writeUtf(conditionUnlockData1.locationId);
            packetBuffer.writeUtf(conditionUnlockData1.type.name());
        }

        packetBuffer.writeUtf(quest.difficult.name());
        packetBuffer.writeUtf(quest.dimension.name());
    }

    public static Quest decode(FriendlyByteBuf packetBuffer) {
        ResourceLocation id = ResourceLocation.tryParse(packetBuffer.readUtf());
        String entityId = packetBuffer.readUtf();
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

            if (type == RewardData.Type.ITEM){
                String item = packetBuffer.readUtf();
                int count =packetBuffer.readInt();
                rewards.add(new RewardData(type, item,count ));
            }else {
                String item = packetBuffer.readUtf();
                String recipeType = packetBuffer.readUtf();
                rewards.add(new UnlockRecipeRewardData(item,recipeType));
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
        Quest quest = new Quest(entityId, description, tasks, rewards, conditionUnlocks,difficult1,dimension1, xp);
        quest.id = id;
        return quest;
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

    public String getEntityId() {
        return entityId;
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
}