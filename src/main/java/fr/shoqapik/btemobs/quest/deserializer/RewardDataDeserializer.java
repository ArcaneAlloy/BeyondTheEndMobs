package fr.shoqapik.btemobs.quest.deserializer;

import com.google.gson.*;
import fr.shoqapik.btemobs.quest.ItemRewardData;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.UnlockOptionDialogRewardData;
import fr.shoqapik.btemobs.quest.UnlockRecipeRewardData;
import fr.shoqapik.btemobs.quest.UnlockZoneRewardData;

import java.lang.reflect.Type;

public class RewardDataDeserializer implements JsonDeserializer<RewardData> {
    @Override
    public RewardData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject object = json.getAsJsonObject();

        RewardData.Type type = RewardData.Type.valueOf(object.get("type").getAsString());
        return switch (type) {

            case ITEM -> new ItemRewardData(
                    object.get("itemId").getAsString(),
                    object.get("count").getAsInt()
            );
            case UNLOCK_OPTION_DIALOG -> new UnlockOptionDialogRewardData(BteNpcType.valueOf(object.get("npcType").getAsString()),object.get("optionDialogId").getAsString());
            case UNLOCK_RECIPE -> new UnlockRecipeRewardData(object.get("recipeId").getAsString(), object.get("recipeType").getAsString());
            case UNLOCK_ZONE-> new UnlockZoneRewardData(object.get("zoneId").getAsString());
            case UNLOCK_SYSTEM,
                    UNLOCK_ABILITY->
                    throw new JsonParseException("Reward type not implemented: " + type);
        };
    }
}