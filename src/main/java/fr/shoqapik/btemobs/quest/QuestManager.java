package fr.shoqapik.btemobs.quest;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.deserializer.RewardDataDeserializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(RewardData.class,new RewardDataDeserializer()).create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile List<Quest> quests = Lists.newArrayList();
    /** Se incrementa cada vez que se recargan las quests (para re-sincronizar el progreso guardado). */
    public static int generation = 0;

    public QuestManager() {
        super(GSON, "quest");
    }
    public static Quest getQuest(String id) {
        if (quests==null || quests.isEmpty())return null;
        return quests.stream().filter((quest -> quest.id.toString().equals(id))).findFirst().orElse(null);
    }
    public static List<Quest> getQuest(ResourceLocation entityId) {
        return quests;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> p_10793_, ResourceManager p_10794_, ProfilerFiller p_10795_) {

        if (p_10793_.isEmpty()) {
            LOGGER.info("[QuestManager] Skipping reload — no dialogs data found (keeping {} existing entries)", quests.size());
            return;
        }

        List<Quest> loaded = new ArrayList<>();
        int disabled = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : p_10793_.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            // Quests de la granja (granja_*): desactivadas. No se cargan, así que no salen en el libro ni en el
            // tracker ni lanzan el aviso de "quest lista". En los mundos que ya las tenían desaparecen al cargar.
            if (isDisabled(resourcelocation)) {
                disabled++;
                continue;
            }
            try {
                Quest quest = GSON.fromJson(entry.getValue(), Quest.class);
                if (quest == null) {
                    LOGGER.info("Skipping loading quest {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                quest.id = resourcelocation;
                loaded.add(quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading quest {}", resourcelocation, jsonparseexception);
            }
        }
        // Se sustituye la lista entera (no clear+add) para no dejar a otro hilo leyendo una lista a medias
        quests = loaded;
        LOGGER.info("[QuestManager] Loaded {} quest ({} farm quests disabled)", quests.size(), disabled);
        QuestRecipeLocks.invalidate();
        generation++;
    }

    /** Prefijo de las quests de la granja, desactivadas por ahora. */
    private static final String DISABLED_PREFIX = "granja_";

    private static boolean isDisabled(ResourceLocation id) {
        return id.getPath().startsWith(DISABLED_PREFIX);
    }

    public static List<Quest> getQuests() {
        return quests;
    }

    /** Cliente remoto: definiciones recibidas del servidor (en un solo jugador/host se usa la lista del servidor). */
    public static void setClientQuests(List<Quest> fromServer) {
        quests = new ArrayList<>(fromServer);
        QuestRecipeLocks.invalidate();
        generation++;
    }
    public static Map<BteNpcType,List<Quest>> getQuestsForType(){
        Map<BteNpcType,List<Quest>> map = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()){
            map.put(type,new ArrayList<>());
        }
        for (Quest quest : getQuests()){
            BteMobsMod.LOGGER.info("BteType :{}",quest.getEntityType());
            map.get(quest.getEntityType()).add(quest);
        }
        return map;
    }
}
