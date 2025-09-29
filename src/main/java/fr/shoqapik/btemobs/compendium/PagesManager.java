package fr.shoqapik.btemobs.compendium;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fr.shoqapik.btemobs.rumors.Rumor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PagesManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<PageCompendium> quests = Lists.newArrayList();

    public PagesManager() {
        super(GSON, "pages");
    }

    public static PageCompendium getPages(ResourceLocation entityId, PageCompendium.UnlockLevel levelActually) {
        for (PageCompendium quest: quests) {
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

                PageCompendium quest = GSON.fromJson(entry.getValue(), PageCompendium.class);
                if (quest == null) {
                    LOGGER.info("Skipping loading rumors {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                quests.add(quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading rumors {}", resourcelocation, jsonparseexception);
            }
        }
        quests.sort(Comparator.comparingInt(PageCompendium::getOrden));

    }
    public static List<PageCompendium> getPages() {
        return quests;
    }
}
