package fr.shoqapik.btemobs.client.gui;

import fr.shoqapik.btemobs.quest.Quest;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

/**
 * Textos traducibles de las quests.
 *
 * Claves en los archivos lang (assets/bte_mobs/lang/*.json), usando el nombre del archivo de la quest:
 *   title.quest.<id>         Título de la quest
 *   desc.quest.<id>          Descripción de la quest
 *   tooltip.quest.<id>       Tooltip del botón de la quest
 *   task.quest.<id>.<n>      Descripción de la tarea n (empezando en 0)
 *
 * Si una clave no existe en el idioma activo (ni en en_us), se usa el texto del JSON de la quest.
 * El campo del JSON también puede contener directamente una clave de traducción.
 */
public final class QuestTexts {

    private QuestTexts() {}

    /** Traduce el texto si es una clave existente; si no, lo devuelve tal cual. */
    public static String tr(String keyOrText) {
        if (keyOrText == null) return "";
        return I18n.exists(keyOrText) ? I18n.get(keyOrText) : keyOrText;
    }

    private static String path(ResourceLocation id) {
        return id == null ? "" : id.getPath();
    }

    public static String title(Quest quest) {
        String key = "title.quest." + path(quest.id);
        if (I18n.exists(key)) return I18n.get(key);
        // Sin traducción: mostramos el id legible en vez de la clave
        String p = path(quest.id).replace('_', ' ');
        return p.isEmpty() ? key : Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    public static String description(Quest quest) {
        String key = "desc.quest." + path(quest.id);
        return I18n.exists(key) ? I18n.get(key) : tr(quest.getDescription());
    }

    public static String tooltip(Quest quest) {
        String key = "tooltip.quest." + path(quest.id);
        return I18n.exists(key) ? I18n.get(key) : tr(quest.getToolTip());
    }

    /**
     * Tooltip de una quest bloqueada: una línea por requisito, en verde si ya se cumple y en rojo si no.
     *   PARENT_QUEST -> "Completa la quest: <título> (<NPC>)"
     *   ADVANCEMENT  -> texto de gui.bte_mobs.quest.requirement.advancement.<ruta> si existe
     *                   (p. ej. minecraft:nether/root -> "Entra en el Nether"); si no, el título del logro o su id.
     */
    public static java.util.List<net.minecraft.network.chat.Component> lockedTooltip(fr.shoqapik.btemobs.capability.QuestStateData state) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        lines.add(net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.locked").withStyle(net.minecraft.ChatFormatting.GOLD));
        for (fr.shoqapik.btemobs.capability.UnlockState us : state.unlockStates) {
            net.minecraft.network.chat.Component text = switch (us.type) {
                case PARENT_QUEST -> net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.requirement.quest", parentQuestName(us.id));
                case ADVANCEMENT -> net.minecraft.network.chat.Component.literal(advancementText(us.id));
                case NETHER_PORTAL -> net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.requirement.nether_portal");
                case END_PORTAL -> net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.requirement.end_portal");
            };
            lines.add(net.minecraft.network.chat.Component.literal(us.unlock ? " \u2714 " : " \u2718 ").append(text)
                    .withStyle(us.unlock ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        }
        return lines;
    }

    /** Tooltip de una opción de diálogo bloqueada (p. ej. mejorar/degradar libros del Warlock): quests que la desbloquean. */
    public static java.util.List<net.minecraft.network.chat.Component> lockedActionTooltip(
            java.util.List<fr.shoqapik.btemobs.capability.UnlockAction> actions, String actionKey) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        lines.add(net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.locked_option").withStyle(net.minecraft.ChatFormatting.GOLD));
        if (actions == null) return lines;
        for (fr.shoqapik.btemobs.capability.UnlockAction action : actions) {
            if (!action.action.equals(actionKey)) continue;
            for (fr.shoqapik.btemobs.capability.UnlockState us : action.unlockStates) {
                net.minecraft.network.chat.Component text = us.type == fr.shoqapik.btemobs.quest.ConditionUnlockData.Type.PARENT_QUEST
                        ? net.minecraft.network.chat.Component.translatable("gui.bte_mobs.quest.requirement.quest", parentQuestName(us.id))
                        : net.minecraft.network.chat.Component.literal(us.id);
                lines.add(net.minecraft.network.chat.Component.literal(us.unlock ? " \u2714 " : " \u2718 ").append(text)
                        .withStyle(us.unlock ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            }
        }
        return lines;
    }

    private static String parentQuestName(String questId) {
        ResourceLocation id = ResourceLocation.tryParse(questId);
        if (id == null) return questId;
        String title;
        String key = "title.quest." + id.getPath();
        if (I18n.exists(key)) {
            title = I18n.get(key);
        } else {
            String p = id.getPath().replace('_', ' ');
            title = p.isEmpty() ? questId : Character.toUpperCase(p.charAt(0)) + p.substring(1);
        }
        // NPC que da la quest (si se conoce)
        String npc = null;
        Quest parent = fr.shoqapik.btemobs.quest.QuestManager.getQuest(questId);
        if (parent != null) {
            npc = I18n.get("entity.bte_mobs." + parent.getEntityType().name().toLowerCase(java.util.Locale.ROOT));
        } else {
            String prefix = id.getPath().contains("_") ? id.getPath().substring(0, id.getPath().indexOf('_')) : "";
            if (java.util.List.of("antonio", "anna", "noah", "oriana").contains(prefix)) {
                npc = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
            }
        }
        return npc != null ? title + " (" + npc + ")" : title;
    }

    private static String advancementText(String advancementId) {
        ResourceLocation id = ResourceLocation.tryParse(advancementId);
        if (id == null) return advancementId;
        String key = "gui.bte_mobs.quest.requirement.advancement." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(key)) return I18n.get(key);
        // Título del logro si el cliente lo conoce
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) {
            net.minecraft.advancements.Advancement adv = mc.getConnection().getAdvancements().getAdvancements().get(id);
            if (adv != null && adv.getDisplay() != null) {
                return I18n.get("gui.bte_mobs.quest.requirement.advancement", adv.getDisplay().getTitle().getString());
            }
        }
        return I18n.get("gui.bte_mobs.quest.requirement.advancement", advancementId);
    }

    public static String task(Quest quest, int index, String fallback) {
        String key = "task.quest." + path(quest.id) + "." + index;
        return I18n.exists(key) ? I18n.get(key) : tr(fallback);
    }
}
