package fr.shoqapik.btemobs.api;

import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.client.gui.QuestScreen;
import fr.shoqapik.btemobs.client.gui.QuestTexts;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestRecipeLocks;
import fr.shoqapik.btemobs.quest.RewardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * API de cliente para otros mods (EnderEyesGUI): qué quest desbloquea una receta de los NPC, cómo se llama, qué le
 * falta para desbloquearse, y abrir esa quest en el libro de quests.
 *
 * EnderEyesGUI la usa por reflexión (sin depender de esta versión del mod), así que los nombres y firmas de estos
 * métodos públicos no deben cambiar.
 */
public final class QuestRecipeLinks {

    private QuestRecipeLinks() {}

    private static RecipeManager cachedManager;
    private static int cachedQuestCount = -1;
    private static Map<ResourceLocation, Quest> cachedLinks = new HashMap<>();

    /** Todas las quests que conoce el cliente (sincronizadas en el capability del jugador). */
    private static List<Map.Entry<Quest, QuestStateData>> clientQuests() {
        List<Map.Entry<Quest, QuestStateData>> list = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return list;
        RecipeCapability<?> cap = RecipeCapability.get(mc.player);
        if (cap == null || cap.quests == null) return list;
        for (Map<Quest, QuestStateData> perNpc : cap.quests.values()) {
            if (perNpc != null) list.addAll(perNpc.entrySet());
        }
        // Orden estable: por NPC y por el orden de progresión de la quest
        list.sort(java.util.Comparator.<Map.Entry<Quest, QuestStateData>>comparingInt(e -> e.getKey().getEntityType().ordinal())
                .thenComparingInt(e -> e.getKey().getOrder()));
        return list;
    }

    /** Recetas -> quest que las desbloquea (se recalcula si cambian las recetas o las quests). */
    private static Map<ResourceLocation, Quest> links() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Map.of();
        RecipeManager manager = mc.level.getRecipeManager();
        List<Map.Entry<Quest, QuestStateData>> entries = clientQuests();
        if (manager == cachedManager && entries.size() == cachedQuestCount) return cachedLinks;
        Collection<Quest> quests = new ArrayList<>();
        for (Map.Entry<Quest, QuestStateData> e : entries) quests.add(e.getKey());
        Map<ResourceLocation, Quest> map = new HashMap<>();
        for (Quest quest : quests) {
            for (RewardData reward : quest.getRewards()) {
                for (Recipe<?> recipe : QuestRecipeLocks.recipesFor(manager, quests, reward)) {
                    map.putIfAbsent(recipe.getId(), quest);
                }
            }
        }
        // Recetas que heredan el bloqueo de su base (p. ej. Cocktail Cutlass <- Diamond Sword)
        for (Map.Entry<ResourceLocation, Quest> e : QuestRecipeLocks.inheritedLocks(manager, quests).entrySet()) {
            map.putIfAbsent(e.getKey(), e.getValue());
        }
        cachedManager = manager;
        cachedQuestCount = entries.size();
        cachedLinks = map;
        return map;
    }

    @Nullable
    private static Map.Entry<Quest, QuestStateData> find(String questId) {
        ResourceLocation id = ResourceLocation.tryParse(questId);
        if (id == null) return null;
        for (Map.Entry<Quest, QuestStateData> e : clientQuests()) {
            if (id.equals(e.getKey().getId())) return e;
        }
        return null;
    }

    /** Id de la quest que desbloquea esta receta (p. ej. "bte_mobs:antonio_jungle_compass"), o null si ninguna. */
    @Nullable
    public static String questForRecipe(ResourceLocation recipeId) {
        Quest quest = links().get(recipeId);
        return quest == null ? null : quest.getId().toString();
    }

    /** Título de la quest con su NPC: "Jungle Compass (Antonio)". */
    @Nullable
    public static Component questTitle(String questId) {
        Map.Entry<Quest, QuestStateData> e = find(questId);
        if (e == null) return null;
        Quest quest = e.getKey();
        Component npc = Component.translatable("entity.bte_mobs." + quest.getEntityType().name().toLowerCase(Locale.ROOT));
        return Component.literal(QuestTexts.title(quest) + " (").append(npc).append(")");
    }

    /** Lo que le falta a la quest para poder hacerse (portal del Nether, otra quest...); vacío si ya está disponible. */
    public static List<Component> questRequirements(String questId) {
        Map.Entry<Quest, QuestStateData> e = find(questId);
        if (e == null) return List.of();
        QuestStateData state = e.getValue();
        boolean unlocked = state.unlockStates.isEmpty() || state.unlockStates.stream().allMatch(u -> u.unlock);
        if (unlocked) return List.of();
        List<Component> lines = QuestTexts.lockedTooltip(state);
        return lines.size() > 1 ? new ArrayList<>(lines.subList(1, lines.size())) : List.of();
    }

    /** ¿La quest ya está completada (aunque falte reclamarla)? */
    public static boolean isQuestComplete(String questId) {
        Map.Entry<Quest, QuestStateData> e = find(questId);
        return e != null && e.getValue().isComplete;
    }

    /** Abre la quest en el libro de quests (solo consulta). Al cerrarlo se vuelve a parent. */
    public static boolean openQuest(String questId, @Nullable Screen parent) {
        Map.Entry<Quest, QuestStateData> e = find(questId);
        if (e == null) return false;
        BteNpcType npc = e.getKey().getEntityType();
        Minecraft.getInstance().setScreen(new QuestScreen(npc, parent).focusQuest(e.getKey().getId()));
        return true;
    }
}
