package fr.shoqapik.btemobs.quests;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class RumorsManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Rumor> quests = Lists.newArrayList();

    public RumorsManager() {
        super(GSON, "rumors");
    }

    public static Rumor getRumor(ResourceLocation entityId, Rumor.UnlockLevel levelActually) {
        for (Rumor quest: quests) {
            if(quest.getTitle().equals(entityId.toString()) && quest.getUnlockLevel().isUnlocked(levelActually)){
                return quest;
            }
        }
        return null;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> p_10793_, ResourceManager p_10794_, ProfilerFiller p_10795_) {
        quests.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : p_10793_.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            try {

                Rumor quest = GSON.fromJson(entry.getValue(), Rumor.class);
                if (quest == null) {
                    LOGGER.info("Skipping loading rumor {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                quests.add(quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading rumor {}", resourcelocation, jsonparseexception);
            }
        }
    }
    public static List<Rumor> getRumors() {
        return quests;
    }
}
