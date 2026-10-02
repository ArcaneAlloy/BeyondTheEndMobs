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
                    object.get("count").getAsInt(),
                    // "nbt" opcional: texto SNBT o un objeto JSON
                    !object.has("nbt") ? null
                            : object.get("nbt").isJsonPrimitive() ? object.get("nbt").getAsString()
                            : object.get("nbt").toString()
            );
            case UNLOCK_OPTION_DIALOG -> new UnlockOptionDialogRewardData(BteNpcType.valueOf(object.get("npcType").getAsString()),object.get("optionDialogId").getAsString());
            case UNLOCK_RECIPE -> new UnlockRecipeRewardData(object.get("recipeId").getAsString(), object.get("recipeType").getAsString(),
                    object.has("requiresQuest") ? object.get("requiresQuest").getAsString() : null);
            case UNLOCK_ZONE-> new UnlockZoneRewardData(object.get("zoneId").getAsString());
            case UNLOCK_RECIPES_BY_INGREDIENT -> new fr.shoqapik.btemobs.quest.IngredientRecipesRewardData(object.get("ingredient").getAsString());
            case UNLOCK_SYSTEM,
                    UNLOCK_ABILITY->
                    throw new JsonParseException("Reward type not implemented: " + type);
        };
    }
}