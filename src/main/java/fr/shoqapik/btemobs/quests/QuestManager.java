package fr.shoqapik.btemobs.quests;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Quest> quests = Lists.newArrayList();

    public QuestManager() {
        super(GSON, "quests");
    }

    public static Quest getQuest(ResourceLocation entityId, Quest.Type type) {
        for (Quest quest: quests) {
            if(quest.getEntityId().toString().equals(entityId.toString()) && quest.getType() == type){
                return quest;
            }
        }
        return null;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> p_10793_, ResourceManager p_10794_, ProfilerFiller p_10795_) {
        // BUG FIX: Solo limpiar y recargar si hay datos — evita borrar datos válidos
        // cuando el servidor dedicado recarga recursos sin incluir los datapacks del mod.
        if (p_10793_.isEmpty()) {
            LOGGER.info("[QuestManager] Skipping reload — no quests data found (keeping {} existing entries)", quests.size());
            return;
        }

        quests.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : p_10793_.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            try {
                Quest quest = GSON.fromJson(entry.getValue(), Quest.class);
                if (quest == null) {
                    LOGGER.info("Skipping loading quest {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                quests.add(quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading quest {}", resourcelocation, jsonparseexception);
            }
        }
        LOGGER.info("[QuestManager] Loaded {} quests", quests.size());
    }

    public static List<Quest> getQuests() {
        return quests;
    }
}
