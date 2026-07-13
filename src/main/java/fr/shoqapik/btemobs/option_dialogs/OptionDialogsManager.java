package fr.shoqapik.btemobs.option_dialogs;

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

public class OptionDialogsManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<OptionDialogs> quests = Lists.newArrayList();

    public OptionDialogsManager() {
        super(GSON, "dialogs");
    }

    public static OptionDialogs getQuest(ResourceLocation entityId, OptionDialogs.Type type) {
        for (OptionDialogs quest: quests) {
            if(quest.getEntityId().toString().equals(entityId.toString()) && quest.getType() == type){
                return quest;
            }
        }
        return null;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> p_10793_, ResourceManager p_10794_, ProfilerFiller p_10795_) {

        if (p_10793_.isEmpty()) {
            LOGGER.info("[OptionDialogsManager] Skipping reload — no dialogs data found (keeping {} existing entries)", quests.size());
            return;
        }

        quests.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : p_10793_.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            try {
                OptionDialogs quest = GSON.fromJson(entry.getValue(), OptionDialogs.class);
                if (quest == null) {
                    LOGGER.info("Skipping loading quest {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                quests.add(quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading quest {}", resourcelocation, jsonparseexception);
            }
        }
        LOGGER.info("[OptionDialogsManager] Loaded {} dialogs", quests.size());
    }

    public static List<OptionDialogs> getQuests() {
        return quests;
    }
}
