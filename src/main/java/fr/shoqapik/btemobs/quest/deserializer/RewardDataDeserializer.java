package fr.shoqapik.btemobs.quest.deserializer;

import com.google.gson.*;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.UnlockRecipeRewardData;

import java.lang.reflect.Type;

public class RewardDataDeserializer implements JsonDeserializer<RewardData> {
    @Override
    public RewardData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject object = json.getAsJsonObject();

        RewardData.Type type = RewardData.Type.valueOf(object.get("type").getAsString());
        return switch (type) {

            case ITEM -> new RewardData(
                    RewardData.Type.ITEM,
                    object.get("itemId").getAsString(),
                    object.get("count").getAsInt()
            );

            case UNLOCK_RECIPE -> new UnlockRecipeRewardData(object.get("itemId").getAsString(), object.get("recipeType").getAsString());

            case UNLOCK_OPTION_DIALOG,
                    UNLOCK_SYSTEM,
                    UNLOCK_ABILITY,
                    UNLOCK_ZONE ->
                    throw new JsonParseException("Reward type not implemented: " + type);
        };
    }
}