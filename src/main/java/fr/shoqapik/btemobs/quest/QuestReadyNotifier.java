package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.StatTaskData;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.QuestReadyPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Avisa al jugador cuando una quest está lista para reclamar (todas sus tareas cumplidas) y le dice al cliente qué
 * NPCs tienen algo que reclamar (para el "!" sobre su cabeza).
 *
 * Se revisa cada segundo por jugador. Las tareas COLLECT (entregar items) se comprueban con el inventario de ESE
 * jugador sin tocar el estado de la quest (en cooperativo el estado es compartido y no debe alternar entre
 * inventarios). El resto de tareas usan su estado guardado, así que en cooperativo avisan a todos los jugadores.
 *
 * Al entrar al mundo no se muestra aviso de las quests que ya estaban listas: solo se envían los "!" de los NPCs.
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestReadyNotifier {

    private QuestReadyNotifier() {}

    /** Última lista de quests listas enviada a cada jugador (null = aún no se ha enviado nada en esta sesión). */
    private static final Map<UUID, Set<String>> LAST_READY = new HashMap<>();

    public static void tick(ServerPlayer player, RecipeCapability<?> cap) {
        Set<String> ready = new LinkedHashSet<>();
        Map<String, BteNpcType> npcOf = new HashMap<>();
        EnumSet<BteNpcType> npcs = EnumSet.noneOf(BteNpcType.class);

        for (Map.Entry<BteNpcType, Map<Quest, QuestStateData>> npcEntry : cap.quests.entrySet()) {
            for (Map.Entry<Quest, QuestStateData> e : npcEntry.getValue().entrySet()) {
                Quest quest = e.getKey();
                QuestStateData state = e.getValue();
                if (quest == null || quest.id == null || state == null || state.isReclaim) continue;
                if (!isReady(player, state)) continue;
                String id = quest.id.toString();
                ready.add(id);
                npcOf.put(id, npcEntry.getKey());
                npcs.add(npcEntry.getKey());
            }
        }

        Set<String> previous = LAST_READY.get(player.getUUID());
        if (previous != null && previous.equals(ready)) return;

        List<String> fresh = new ArrayList<>();
        List<BteNpcType> freshNpcs = new ArrayList<>();
        if (previous != null) {
            for (String id : ready) {
                if (!previous.contains(id)) {
                    fresh.add(id);
                    freshNpcs.add(npcOf.get(id));
                }
            }
        }
        LAST_READY.put(player.getUUID(), ready);
        BteMobsMod.sendToClient(new QuestReadyPacket(fresh, freshNpcs, npcs), player);
    }

    /** true si la quest está desbloqueada y todas sus tareas están cumplidas para este jugador. */
    private static boolean isReady(ServerPlayer player, QuestStateData state) {
        boolean unlocked = state.unlockStates == null || state.unlockStates.isEmpty()
                || state.unlockStates.stream().allMatch(u -> u.unlock);
        if (!unlocked || state.statTaskData == null || state.statTaskData.isEmpty()) return false;
        for (StatTaskData task : state.statTaskData) {
            if (task.taskType == TaskData.Type.COLLECT) {
                if (RecipeCapability.countItems(player, task.id) < task.maxCount) return false;
            } else if (!task.complete) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_READY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // El cliente conserva los "!"; no hace falta nada. Se deja por si en el futuro se filtra por dimensión.
    }
}
