package fr.shoqapik.btemobs.capability;


import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.RecipeSharing;
import fr.shoqapik.btemobs.ServerData;
import fr.shoqapik.btemobs.api.RecipePlayer;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.SyncRecipeManager;
import fr.shoqapik.btemobs.quest.*;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class RecipeCapability<T extends Recipe<?>> implements RecipePlayer<T> {
    public Player player;
    private Map<RecipeType<T>, List<T>> recipeManager = new HashMap<>();
    public boolean dirty = false;
    private Level level ;
    public Map<BteNpcType,Map<Quest,QuestStateData>> quests=new HashMap<>();
    public List<UnlockAction> unlockActions = new ArrayList<>();
    public List<UnlockAction> unlockZone = new ArrayList<>();
    public static RecipeCapability get(Player player){
        return player.getCapability(BteCapability.RECIPE_CAPABILITY,null).orElse(null);
    }
    public Map<Quest,QuestStateData> getQuestForNpc(BteNpcType npcType){
        return quests.get(npcType);
    }
    /** Cantidad de items del inventario del jugador que cumplen la especificación ("mod:item" o "#mod:tag"). */
    public static int countItems(Player player, String spec) {
        int total = 0;
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inv.getItem(i);
            if (QuestTriggers.matchesItem(spec, stack)) total += stack.getCount();
        }
        return total;
    }

    /**
     * Recalcula las tareas COLLECT con el inventario de ESTE jugador y si cada quest está completa.
     * En cooperativo el estado es compartido: muestra el progreso del último jugador que lo ha revisado
     * (al abrir la ventana de quests se revisa con el inventario de quien la abre). Al reclamar se vuelve
     * a comprobar con el inventario de quien reclama.
     */
    public void checkChangedInventory(){
        if (this.player == null) return;
        boolean changed = false;
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData data : questMap.values()) {
                if (data.isReclaim) continue;
                boolean unlock = data.unlockStates.isEmpty() || data.unlockStates.stream().allMatch(e -> e.unlock);
                if (!unlock) continue;
                boolean allDone = true;
                for (StatTaskData task : data.statTaskData) {
                    if (task.taskType == TaskData.Type.COLLECT) {
                        int count = Math.min(task.maxCount, countItems(this.player, task.id));
                        if (count != task.count) {
                            task.count = count;
                            changed = true;
                        }
                        if (count < task.maxCount) allDone = false;
                    } else if (!task.complete) {
                        allDone = false;
                    }
                }
                if (data.isComplete != allDone) {
                    data.isComplete = allDone;
                    changed = true;
                }
            }
        }
        if (changed) this.dirty = true;
    }

    /** Marca como cumplidos los requisitos PARENT_QUEST que dependen de esta quest. */
    public void completeQuest(Quest quest){
        completeParent(this.quests, this.unlockActions, quest.id.toString());
        this.dirty = true;
    }

    private static void completeParent(Map<BteNpcType, Map<Quest, QuestStateData>> all, List<UnlockAction> actions, String questId) {
        for (Map<Quest, QuestStateData> questMap : all.values()) {
            for (QuestStateData data : questMap.values()) {
                for (UnlockState state : data.unlockStates) {
                    if (state.type == ConditionUnlockData.Type.PARENT_QUEST && state.id.equals(questId)) {
                        state.unlock = true;
                    }
                }
            }
        }
        for (UnlockAction unlockAction : actions){
            for (UnlockState state : unlockAction.unlockStates) {
                if (state.type == ConditionUnlockData.Type.PARENT_QUEST && state.id.equals(questId)) {
                    state.unlock = true;
                }
            }
        }
    }

    public void hunterQuestUpdate(LivingDeathEvent event){
        String entityId = event.getEntity().getEncodeId();
        if (entityId == null) return;
        net.minecraft.world.entity.EntityType<?> entityType = event.getEntity().getType();
        java.util.function.Predicate<StatTaskData> kill = task -> {
            if (task.id == null) return false;
            // "#mod:tag" = cualquier entidad del tag de entidades (p. ej. los Iceologer de varios mods)
            if (task.id.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(task.id.substring(1));
                if (tagId == null || !entityType.is(net.minecraft.tags.TagKey.create(net.minecraft.core.Registry.ENTITY_TYPE_REGISTRY, tagId))) return false;
            } else if (!entityId.equals(task.id)) return false;
            task.count++;
            return true;
        };
        updateTasks(TaskData.Type.HUNTER, kill);
        updateTasks(TaskData.Type.BOSS_HUNTER, kill);
    }

    /** Estado guardado de una quest por su id (o null). */
    public QuestStateData findState(ResourceLocation id) {
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().id.equals(id)) return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Actualiza las tareas de un tipo en todas las quests desbloqueadas y no completadas.
     * El updater devuelve true si ha cambiado el progreso de esa tarea.
     */
    public void updateTasks(TaskData.Type type, java.util.function.Predicate<StatTaskData> updater) {
        boolean changed = false;
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData state : questMap.values()) {
                if (state.isComplete || state.isReclaim) continue;
                boolean unlock = state.unlockStates.isEmpty() || state.unlockStates.stream().allMatch(e -> e.unlock);
                if (!unlock) continue;
                for (StatTaskData task : state.statTaskData) {
                    if (task.complete || task.taskType != type) continue;
                    if (updater.test(task)) {
                        task.count = Math.min(task.count, task.maxCount);
                        if (task.count >= task.maxCount) task.complete = true;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            this.dirty = true;
            // Recalcula si alguna quest ha quedado completa (tiene en cuenta también las tareas COLLECT)
            checkChangedInventory();
        }
    }

    /**
     * Desbloquea los requisitos ADVANCEMENT que el jugador ya tiene (p. ej. minecraft:nether/root).
     * Llamado periódicamente desde QuestTriggers.tickPlayer.
     */
    public void checkAdvancementConditions(ServerPlayer player) {
        boolean changed = false;
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData state : questMap.values()) {
                for (UnlockState us : state.unlockStates) {
                    if (!us.unlock && (us.type == ConditionUnlockData.Type.NETHER_PORTAL || us.type == ConditionUnlockData.Type.END_PORTAL)
                            && isPortalOpen(player, us.type == ConditionUnlockData.Type.END_PORTAL ? END_PORTAL_EYES : NETHER_PORTAL_EYES)) {
                        us.unlock = true;
                        changed = true;
                        continue;
                    }
                    if (us.unlock || us.type != ConditionUnlockData.Type.ADVANCEMENT) continue;
                    ResourceLocation advId = ResourceLocation.tryParse(us.id);
                    if (advId == null) continue;
                    net.minecraft.advancements.Advancement adv = player.getServer().getAdvancements().getAdvancement(advId);
                    if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                        us.unlock = true;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            this.dirty = true;
            checkChangedInventory();
        }
    }

    /**
     * Tareas BOSS_HUNTER que se cumplen con un logro oculto de bte_mobs (data/bte_mobs/advancements/kills/<mod>/<entidad>.json).
     * El logro se gana al matar al jefe aunque la quest aún no esté desbloqueada, así que la muerte no se pierde
     * (p. ej. el Lich del Infested Temple, que solo aparece una vez por templo). Las tareas sin logro siguen
     * funcionando solo con la muerte (hunterQuestUpdate).
     */
    public void checkBossKillAdvancements(ServerPlayer player) {
        boolean changed = false;
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData state : questMap.values()) {
                if (state.isComplete || state.isReclaim) continue;
                boolean unlock = state.unlockStates.isEmpty() || state.unlockStates.stream().allMatch(e -> e.unlock);
                if (!unlock) continue;
                for (StatTaskData task : state.statTaskData) {
                    if (task.complete || task.taskType != TaskData.Type.BOSS_HUNTER || task.id == null) continue;
                    ResourceLocation entityId = ResourceLocation.tryParse(task.id);
                    if (entityId == null) continue;
                    ResourceLocation advId = new ResourceLocation(BteMobsMod.MODID, "kills/" + entityId.getNamespace() + "/" + entityId.getPath());
                    net.minecraft.advancements.Advancement adv = player.getServer().getAdvancements().getAdvancement(advId);
                    if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                        task.count = task.maxCount;
                        task.complete = true;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            this.dirty = true;
            checkChangedInventory();
        }
    }

    /** Ender Eyes con los que enders_journey abre físicamente los portales del Forgotten Realm. */
    public static final int NETHER_PORTAL_EYES = 8;
    public static final int END_PORTAL_EYES = 16;

    /**
     * true si el portal está abierto: enders_journey abre el del Nether con 8 Ender Eyes conseguidos y el del End con 16.
     * En cooperativo basta con que lo cumpla un jugador (el estado de las quests es compartido, igual que el portal).
     */
    private static boolean isPortalOpen(ServerPlayer player, int eyes) {
        try {
            return mc.duzo.ender_journey.capabilities.PortalPlayer.get(player)
                    .map(p -> p.getEyesEarn() >= eyes).orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Si los requisitos del JSON de una quest han cambiado respecto a lo guardado en el jugador,
     * se regeneran conservando el estado de los que siguen igual. Un PARENT_QUEST nuevo cuenta
     * como cumplido si el jugador ya reclamó esa quest.
     */
    private static boolean reconcileUnlockStates(Map<BteNpcType, Map<Quest, QuestStateData>> all) {
        boolean changed = false;
        Map<String, QuestStateData> byId = new HashMap<>();
        for (Map<Quest, QuestStateData> m : all.values())
            for (Map.Entry<Quest, QuestStateData> e : m.entrySet()) if (e.getKey() != null) byId.put(e.getKey().id.toString(), e.getValue());

        for (Map<Quest, QuestStateData> m : all.values()) {
            for (Map.Entry<Quest, QuestStateData> e : m.entrySet()) {
                if (e.getKey() == null) continue;
                List<ConditionUnlockData> defs = e.getKey().getConditionUnlockData();
                List<UnlockState> saved = e.getValue().unlockStates;
                boolean same = defs.size() == saved.size();
                for (int i = 0; same && i < defs.size(); i++) {
                    same = defs.get(i).type == saved.get(i).type && defs.get(i).locationId.equals(saved.get(i).id);
                }
                if (same) continue;
                List<UnlockState> fresh = new ArrayList<>();
                for (ConditionUnlockData d : defs) {
                    UnlockState old = saved.stream().filter(s -> s.type == d.type && s.id.equals(d.locationId)).findFirst().orElse(null);
                    boolean unlocked = old != null && old.unlock;
                    if (!unlocked && d.type == ConditionUnlockData.Type.PARENT_QUEST) {
                        QuestStateData parent = byId.get(d.locationId);
                        unlocked = parent != null && parent.isReclaim;
                    }
                    fresh.add(new UnlockState(d.locationId, d.type, unlocked));
                }
                e.getValue().unlockStates = fresh;
                changed = true;
            }
        }
        return changed;
    }

    /** true si hay alguna tarea pendiente de ese tipo (para no hacer trabajo innecesario). */
    public boolean hasPendingTask(TaskData.Type type) {
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (QuestStateData state : questMap.values()) {
                if (state.isComplete || state.isReclaim) continue;
                for (StatTaskData task : state.statTaskData) {
                    if (!task.complete && task.taskType == type) return true;
                }
            }
        }
        return false;
    }

    /** Las tareas guardadas coinciden con la definición actual de la quest (mismo número, tipo, id y cantidad). */
    private static boolean tasksMatch(Quest quest, QuestStateData state) {
        List<TaskData> defs = quest.getTasks();
        if (defs.size() != state.statTaskData.size()) return false;
        for (int i = 0; i < defs.size(); i++) {
            TaskData d = defs.get(i);
            StatTaskData s = state.statTaskData.get(i);
            if (d.type != s.taskType || !d.getEntityIdLocation().equals(s.id) || d.count != s.maxCount) return false;
        }
        return true;
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    public boolean getQuestComplete(Quest quest){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey().id.equals(quest.id)) {
                    return entry.getValue().isComplete;
                }
            }
        }
        return false;
    }

    public boolean getQuestReclaim(Quest quest){
        for (Map<Quest, QuestStateData> questMap : quests.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey().id.equals(quest.id)) {
                    return entry.getValue().isReclaim;
                }
            }
        }
        return false;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    // ------------------------------------------------------------------ Modo cooperativo

    /** Se incrementa cada vez que cambia el estado compartido; cada jugador se resincroniza al verlo cambiar. */
    private static int sharedVersion = 0;
    private int seenSharedVersion = -1;

    /** Fuerza reenviar el estado a su cliente en el siguiente tick (el cliente crea un jugador nuevo al cambiar de dimensión o reaparecer). */
    public void requestResync() {
        this.seenSharedVersion = -1;
    }
    /** true cuando this.quests / this.unlockActions apuntan al estado compartido del mundo (ServerData). */
    private boolean sharedBound = false;

    @Override
    public void tick(Player player) {
        if (level == null || level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!sharedBound) bindShared(serverPlayer);

        ServerData serverData = ServerData.get();
        if (serverData.questGeneration != QuestManager.generation) {
            if (syncWithQuestManager(this.quests, this.unlockActions)) this.dirty = true;
            serverData.questGeneration = QuestManager.generation;
        }

        if (this.dirty) {
            sharedVersion++;
            this.dirty = false;
        }
        if (this.seenSharedVersion != sharedVersion) {
            this.seenSharedVersion = sharedVersion;
            BteMobsMod.sendToClient(new SyncRecipeManager(player.getId(), this.serializeNBT(), false), serverPlayer);
        }
    }

    /**
     * Enlaza las quests de este jugador con las del mundo. El primer jugador (o una partida antigua) aporta su
     * progreso como estado inicial; el resto lo fusiona (quests reclamadas, contadores, requisitos cumplidos).
     * También comparte sus recetas con los demás y recibe las ya desbloqueadas.
     */
    private void bindShared(ServerPlayer player) {
        ServerData serverData = ServerData.get();
        Map<BteNpcType, Map<Quest, QuestStateData>> shared = serverData.getSharedQuests();
        if (shared == null) {
            serverData.setSharedQuests(this.quests, this.unlockActions);
        } else if (shared != this.quests) {
            List<String> reclaimed = mergeInto(this.quests, this.unlockActions, shared, serverData.getSharedUnlockActions());
            this.quests = shared;
            this.unlockActions = serverData.getSharedUnlockActions();
            for (String id : reclaimed) completeParent(this.quests, this.unlockActions, id);
            // Revalida quests/acciones tras la fusión (p. ej. requisitos antiguos que traía la partida de este jugador)
            serverData.questGeneration = -1;
        }
        this.sharedBound = true;
        RecipeSharing.shareFromPlayer(player);
        RecipeSharing.syncPlayer(player);
        this.dirty = true;
    }

    /** Fusiona el progreso de un jugador en el compartido. Devuelve los ids de quests que pasan a estar reclamadas. */
    private static List<String> mergeInto(Map<BteNpcType, Map<Quest, QuestStateData>> mine, List<UnlockAction> myActions,
                                          Map<BteNpcType, Map<Quest, QuestStateData>> shared, List<UnlockAction> sharedActions) {
        List<String> reclaimed = new ArrayList<>();
        Map<String, QuestStateData> sharedById = new HashMap<>();
        for (Map<Quest, QuestStateData> m : shared.values())
            for (Map.Entry<Quest, QuestStateData> e : m.entrySet()) if (e.getKey() != null) sharedById.put(e.getKey().id.toString(), e.getValue());

        for (Map.Entry<BteNpcType, Map<Quest, QuestStateData>> typeEntry : mine.entrySet()) {
            for (Map.Entry<Quest, QuestStateData> e : typeEntry.getValue().entrySet()) {
                if (e.getKey() == null) continue;
                String id = e.getKey().id.toString();
                QuestStateData m = e.getValue();
                QuestStateData s = sharedById.get(id);
                if (s == null) {
                    shared.computeIfAbsent(typeEntry.getKey(), k -> new HashMap<>()).put(e.getKey(), m);
                    if (m.isReclaim) reclaimed.add(id);
                    continue;
                }
                if (s.isReclaim) continue;
                if (m.isReclaim) {
                    s.isReclaim = true;
                    s.isComplete = true;
                    reclaimed.add(id);
                    continue;
                }
                if (m.statTaskData.size() == s.statTaskData.size()) {
                    for (int i = 0; i < m.statTaskData.size(); i++) {
                        StatTaskData mt = m.statTaskData.get(i);
                        StatTaskData st = s.statTaskData.get(i);
                        if (mt.taskType != st.taskType || !mt.id.equals(st.id) || mt.taskType == TaskData.Type.COLLECT) continue;
                        st.seen.addAll(mt.seen);
                        st.count = Math.min(st.maxCount, Math.max(Math.max(st.count, mt.count), st.seen.size()));
                        st.complete = st.complete || mt.complete || st.count >= st.maxCount;
                    }
                }
                for (UnlockState ms : m.unlockStates) {
                    if (!ms.unlock) continue;
                    for (UnlockState ss : s.unlockStates) {
                        if (ss.type == ms.type && ss.id.equals(ms.id)) ss.unlock = true;
                    }
                }
            }
        }
        for (UnlockAction ma : myActions) {
            UnlockAction sa = sharedActions.stream().filter(a -> a.action.equals(ma.action)).findFirst().orElse(null);
            if (sa == null) {
                sharedActions.add(ma);
                continue;
            }
            for (UnlockState ms : ma.unlockStates) {
                if (!ms.unlock) continue;
                for (UnlockState ss : sa.unlockStates) {
                    if (ss.type == ms.type && ss.id.equals(ms.id)) ss.unlock = true;
                }
            }
        }
        return reclaimed;
    }

    /**
     * Ajusta el mapa de quests a las quests cargadas ahora mismo (tras arrancar o hacer /reload):
     * añade las nuevas, quita las desactivadas y actualiza las tareas/requisitos que hayan cambiado.
     */
    public static boolean syncWithQuestManager(Map<BteNpcType, Map<Quest, QuestStateData>> all, List<UnlockAction> actions) {
        List<Quest> current = QuestManager.getQuests();
        if (current.isEmpty()) return false;
        boolean changed = false;
        Map<String, QuestStateData> byId = new HashMap<>();
        for (Map<Quest, QuestStateData> m : all.values())
            for (Map.Entry<Quest, QuestStateData> e : m.entrySet()) if (e.getKey() != null) byId.putIfAbsent(e.getKey().id.toString(), e.getValue());

        Map<BteNpcType, Map<Quest, QuestStateData>> fresh = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()) fresh.put(type, new HashMap<>());
        for (Quest quest : current) {
            QuestStateData state = byId.get(quest.id.toString());
            if (state == null) {
                state = newState(quest, actions);
                changed = true;
            } else if (!state.isComplete && !state.isReclaim && !tasksMatch(quest, state)) {
                state.statTaskData = freshTasks(quest);
                changed = true;
            }
            fresh.computeIfAbsent(quest.getEntityType(), k -> new HashMap<>()).put(quest, state);
        }
        for (Map.Entry<BteNpcType, Map<Quest, QuestStateData>> e : fresh.entrySet()) {
            Map<Quest, QuestStateData> old = all.get(e.getKey());
            if (old == null || old.size() != e.getValue().size() || !old.keySet().containsAll(e.getValue().keySet())) changed = true;
        }
        if (changed) {
            for (Map.Entry<BteNpcType, Map<Quest, QuestStateData>> e : fresh.entrySet()) {
                Map<Quest, QuestStateData> target = all.computeIfAbsent(e.getKey(), k -> new HashMap<>());
                target.clear();
                target.putAll(e.getValue());
            }
        }
        if (reconcileUnlockStates(all)) changed = true;
        if (ensureDialogActions(all, actions)) changed = true;
        return changed;
    }

    /**
     * Garantiza que cada recompensa UNLOCK_OPTION_DIALOG tenga su acción bloqueada registrada, también en partidas
     * guardadas antes de añadir la recompensa a la quest. Si la quest ya estaba reclamada, la opción queda desbloqueada.
     */
    private static boolean ensureDialogActions(Map<BteNpcType, Map<Quest, QuestStateData>> all, List<UnlockAction> actions) {
        boolean changed = false;
        // Quita requisitos de quests que ya no dan esa recompensa (p. ej. si se movió a otra quest)
        java.util.Set<String> valid = new java.util.HashSet<>();
        for (Map<Quest, QuestStateData> m : all.values())
            for (Quest q : m.keySet()) {
                if (q == null) continue;
                for (RewardData r : q.getRewards())
                    if (r.type == RewardData.Type.UNLOCK_OPTION_DIALOG) valid.add(r.getObjectId() + "|" + q.id);
            }
        for (UnlockAction action : actions) {
            if (action.unlockStates.removeIf(st -> st.type == ConditionUnlockData.Type.PARENT_QUEST
                    && !valid.contains(action.action + "|" + st.id))) changed = true;
        }
        for (Map<Quest, QuestStateData> m : all.values()) {
            for (Map.Entry<Quest, QuestStateData> e : m.entrySet()) {
                if (e.getKey() == null) continue;
                String questId = e.getKey().id.toString();
                for (RewardData reward : e.getKey().getRewards()) {
                    if (reward.type != RewardData.Type.UNLOCK_OPTION_DIALOG) continue;
                    UnlockAction action = actions.stream().filter(a -> a.action.equals(reward.getObjectId())).findAny().orElse(null);
                    if (action == null) {
                        action = new UnlockAction(reward.getObjectId(), new ArrayList<>());
                        actions.add(action);
                    }
                    boolean present = action.unlockStates.stream().anyMatch(st -> st.type == ConditionUnlockData.Type.PARENT_QUEST && st.id.equals(questId));
                    if (!present) {
                        action.unlockStates.add(new UnlockState(questId, ConditionUnlockData.Type.PARENT_QUEST, e.getValue().isReclaim));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static List<StatTaskData> freshTasks(Quest quest) {
        List<StatTaskData> tasks = new ArrayList<>();
        for (TaskData data : quest.getTasks()) {
            tasks.add(new StatTaskData(data.getEntityIdLocation(), 0, data.count, false, data.description, data.type));
        }
        return tasks;
    }

    /** Estado inicial de una quest (y registra sus acciones de diálogo bloqueadas). */
    private static QuestStateData newState(Quest quest, List<UnlockAction> actions) {
        List<StatRewardData> dataList = new ArrayList<>();
        List<UnlockState> unlockStates = new ArrayList<>();
        String questId = quest.id.toString();
        for (RewardData rewardData : quest.getRewards()){
            if (rewardData.type == RewardData.Type.UNLOCK_OPTION_DIALOG){
                UnlockAction unlockAction = actions.stream().filter(data -> data.action.equals(rewardData.getObjectId())).findAny().orElse(null);
                if (unlockAction == null){
                    unlockAction = new UnlockAction(rewardData.getObjectId(), new ArrayList<>());
                    actions.add(unlockAction);
                }
                boolean present = unlockAction.unlockStates.stream().anyMatch(st -> st.type == ConditionUnlockData.Type.PARENT_QUEST && st.id.equals(questId));
                if (!present) unlockAction.unlockStates.add(new UnlockState(questId, ConditionUnlockData.Type.PARENT_QUEST, false));
            }
            dataList.add(new StatRewardData(rewardData.getObjectId(),0,rewardData.count,false,rewardData.type));
        }
        for (ConditionUnlockData unlockData : quest.getConditionUnlockData()){
            unlockStates.add(new UnlockState(unlockData.locationId,unlockData.type,false));
        }
        return new QuestStateData(false, false, dataList, freshTasks(quest), unlockStates);
    }

    public void copyFrom(RecipeCapability cap){
        this.recipeManager = cap.recipeManager;
        this.quests = cap.quests;
        this.unlockActions = cap.unlockActions;
        // Si el anterior ya estaba enlazado al estado compartido no hace falta volver a fusionar ni resincronizar recetas
        this.sharedBound = cap.sharedBound;
        this.dirty = true;
    }

    @Override
    public void onJoinGame(Player player, EntityJoinLevelEvent event) {

    }

    @Override
    public void setRecipesForType(RecipeType<T> type, List<T> recipes) {
        Map<RecipeType<T>,List<T>> map = new HashMap<>(this.getRecipeManager());
        map.put(type,new ArrayList<>(recipes));
        this.recipeManager = map;
    }

    @Override
    public void addRecipeForType(RecipeType<T> type, T recipe) {
        List<T> list = this.recipeManager.computeIfAbsent(type, k -> new ArrayList<>());
        boolean present = list.stream().anyMatch(r -> r.getId().equals(recipe.getId()));
        if (!present) {
            list.add(recipe);
            this.dirty = true;
        }
    }

    public void addRecipesForType(RecipeType<T> type, List<T> recipes) {
        for (T r : recipes) addRecipeForType(type, r);
    }

    public void setRecipeManager(Map<RecipeType<T>,List<T>> map){
        this.recipeManager = map;
    }

    @Override
    public Map<RecipeType<T>, List<T>> getRecipeManager() {
        return this.recipeManager;
    }

    @Override
    public List<T> getRecipesForType(RecipeType<T> type) {
        return getRecipeManager().get(type);
    }

    @Override
    public void init(Player player,Level level) {
        this.setPlayer(player);
        this.level = level;

        if(!this.level.isClientSide){
            this.initQuest();
            this.dirty = true;
        }
    }

    public void completeQuest(ResourceLocation id){
        QuestStateData state = findState(id);
        if (state != null) state.isReclaim = true;
        dirty = true;
    }

    public void initQuest(){
        Map<BteNpcType,Map<Quest,QuestStateData>> map = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()){
            map.put(type,new HashMap<>());
        }
        List<UnlockAction> actions = new ArrayList<>();
        for (Quest quest : QuestManager.getQuests()){
            map.computeIfAbsent(quest.getEntityType(), k -> new HashMap<>()).put(quest, newState(quest, actions));
        }
        this.quests = map;
        this.unlockActions = actions;
    }

    // ------------------------------------------------------------------ NBT

    /** Guarda quests + acciones de diálogo (se usa para el jugador y para el estado compartido del mundo). */
    public static CompoundTag saveQuests(Map<BteNpcType, Map<Quest, QuestStateData>> quests, List<UnlockAction> unlockActions) {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        quests.forEach((type, questMap) -> {
            if (type == null) return;
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type.name());
            ListTag tags = new ListTag();
            questMap.forEach((quest, state) -> {
                if (quest == null) return;
                CompoundTag tag1 = new CompoundTag();
                tag1.putString("id", quest.id.toString());
                tag1.put("data", state.save());
                tags.add(tag1);
            });
            tag.put("list", tags);
            list.add(tag);
        });
        nbt.put("quests", list);
        ListTag listTag = new ListTag();
        if (unlockActions != null) {
            for (UnlockAction unlockAction : unlockActions) listTag.add(unlockAction.save());
        }
        nbt.put("unlockAction", listTag);
        return nbt;
    }

    public static Map<BteNpcType, Map<Quest, QuestStateData>> loadQuests(CompoundTag nbt) {
        Map<BteNpcType,Map<Quest,QuestStateData>> map1 = new HashMap<>();
        for (BteNpcType type : BteNpcType.values()){
            map1.put(type,new HashMap<>());
        }
        if (nbt.contains("quests")){
            ListTag listTag = nbt.getList("quests",10);
            for (int i = 0 ; i < listTag.size() ; i++){
                CompoundTag nbt1 = listTag.getCompound(i);
                if (!nbt1.contains("list")) continue;
                ListTag listTag1 = nbt1.getList("list",10);
                for (int j = 0;j < listTag1.size() ; j++){
                    CompoundTag nbt2 = listTag1.getCompound(j);
                    Quest quest = QuestManager.getQuest(nbt2.getString("id"));
                    if (quest == null) continue; // quest renombrada/eliminada/desactivada
                    QuestStateData state = new QuestStateData(nbt2.getCompound("data"));
                    // Si el JSON de la quest ha cambiado sus tareas y aún no está completada, se regeneran
                    if (!state.isComplete && !state.isReclaim && !tasksMatch(quest, state)) {
                        state.statTaskData = freshTasks(quest);
                    }
                    map1.computeIfAbsent(quest.getEntityType(), k -> new HashMap<>()).put(quest, state);
                }
            }
        }
        reconcileUnlockStates(map1);
        return map1;
    }

    public static List<UnlockAction> loadUnlockActions(CompoundTag nbt) {
        List<UnlockAction> unlockActions = new ArrayList<>();
        if (nbt.contains("unlockAction")){
            ListTag list = nbt.getList("unlockAction",10);
            for (int i = 0 ; i < list.size() ; i++){
                unlockActions.add(new UnlockAction(list.getCompound(i)));
            }
        }
        return unlockActions;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = saveQuests(this.quests, this.unlockActions);
        if(!getRecipeManager().isEmpty()){
            ListTag list = new ListTag();
            getRecipeManager().forEach((key,value)->{
                CompoundTag tag = new CompoundTag();
                if (ForgeRegistries.RECIPE_TYPES.getKey(key)!=null){
                    tag.putString("type",ForgeRegistries.RECIPE_TYPES.getKey(key).toString());
                    ListTag list1 = new ListTag();
                    value.forEach(recipe -> {
                        CompoundTag tag1 = new CompoundTag();
                        tag1.putString("recipe",recipe.getId().toString());
                        list1.add(tag1);
                    });
                    tag.put("recipes",list1);
                    list.add(tag);
                }
            });
            nbt.put("manager",list);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        Map<RecipeType<T>,List<T>> map = new HashMap<>();
        if(nbt.contains("manager")){
            ListTag list = nbt.getList("manager",10);
            for (int i = 0 ; i < list.size() ; i ++){
                CompoundTag tag = list.getCompound(i);
                if (!ResourceLocation.isValidResourceLocation(tag.getString("type")))continue;
                RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(tag.getString("type")));
                if(type==null)continue;
                List<T> recipes = new ArrayList<>();
                if(tag.contains("recipes")){
                    ListTag list1 = tag.getList("recipes",10);
                    for (int j = 0 ; j < list1.size() ; j++){
                        CompoundTag tag1 = list1.getCompound(j);
                        Optional<? extends T> recipe = (Optional<? extends T>) this.level.getRecipeManager().byKey(new ResourceLocation(tag1.getString("recipe")));
                        recipe.ifPresent(recipes::add);
                    }
                }
                map.put((RecipeType<T>) type,recipes);
            }
        }
        this.unlockActions = loadUnlockActions(nbt);
        this.recipeManager = map;
        this.quests = loadQuests(nbt);
        this.sharedBound = false;
        this.dirty = true;
    }

    public boolean isUnlockAction(String id) {
        for (UnlockAction unlockAction : unlockActions){
            if (unlockAction.action.equals(id)){
                return unlockAction.unlockStates.isEmpty() || unlockAction.unlockStates.stream().allMatch((unlockState -> unlockState.unlock));
            }
        }
        return true;
    }

    public static class RecipeProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final LazyOptional<RecipePlayer> instance = LazyOptional.of(RecipeCapability::new);

        @NonNull
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BteCapability.RECIPE_CAPABILITY.orEmpty(cap,instance.cast());
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) instance.orElseThrow(NullPointerException::new).serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.orElseThrow(NullPointerException::new).deserializeNBT(nbt);
        }
    }
}
