package fr.shoqapik.btemobs.client;

import fr.shoqapik.btemobs.quest.WelcomeHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.StatTaskData;
import fr.shoqapik.btemobs.client.gui.QuestTexts;
import fr.shoqapik.btemobs.config.BteMobsClientConfig;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestTriggers;
import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Seguimiento de quests (cliente), al estilo del panel de objetivos de los MMO.
 *
 *  - El jugador marca quests con la casilla "Seguir" de la pantalla de quests (hablando con el NPC o desde el libro
 *    de quests del inventario). Hay un máximo (config: maxTrackedQuests, 5 por defecto).
 *  - Las quests seguidas se muestran a la izquierda de la pantalla con sus tareas y progreso.
 *  - Una tecla (Ctrl+X por defecto) oculta o muestra el panel.
 *  - La lista se guarda por mundo / servidor en config/bte_mobs-tracked-quests.json.
 *  - Las quests ya reclamadas dejan de seguirse solas.
 */
public final class QuestTracker {

    private QuestTracker() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "bte_mobs-tracked-quests.json";

    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.bte_mobs.toggle_tracker",
            KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.bte_mobs");

    /** Quests seguidas en el mundo / servidor actual, en el orden en que se añadieron. */
    private static final Set<String> TRACKED = new LinkedHashSet<>();
    private static String worldKey = null;

    private static final int PANEL_WIDTH = 170;
    private static final int LINE_HEIGHT = 10;

    public enum ToggleResult { ADDED, REMOVED, LIMIT }

    // ------------------------------------------------------------------ API

    public static boolean isTracked(Quest quest) {
        return quest != null && TRACKED.contains(quest.id.toString());
    }

    public static int count() {
        return TRACKED.size();
    }

    public static int max() {
        return BteMobsClientConfig.TRACKER_MAX_QUESTS.get();
    }

    public static ToggleResult toggle(Quest quest) {
        String id = quest.id.toString();
        if (TRACKED.remove(id)) {
            save();
            return ToggleResult.REMOVED;
        }
        if (TRACKED.size() >= max()) return ToggleResult.LIMIT;
        TRACKED.add(id);
        // Si estaba oculto, al seguir una quest nueva se vuelve a mostrar
        if (!BteMobsClientConfig.TRACKER_VISIBLE.get()) setVisible(true);
        save();
        return ToggleResult.ADDED;
    }

    private static void setVisible(boolean visible) {
        BteMobsClientConfig.TRACKER_VISIBLE.set(visible);
        BteMobsClientConfig.SPEC.save();
    }

    // ------------------------------------------------------------------ Guardado por mundo / servidor

    private static String currentWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return "sp:" + root.getFileName();
        }
        ServerData server = mc.getCurrentServer();
        return server != null ? "mp:" + server.ip : null;
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE);
    }

    private static JsonObject readAll() {
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement el = GSON.fromJson(reader, JsonElement.class);
                if (el != null && el.isJsonObject()) return el.getAsJsonObject();
            } catch (Exception e) {
                BteMobsMod.LOGGER.warn("No se ha podido leer {}: {}", path, e.getMessage());
            }
        }
        return new JsonObject();
    }

    private static void load() {
        TRACKED.clear();
        worldKey = currentWorldKey();
        if (worldKey == null) return;
        JsonObject all = readAll();
        if (all.has(worldKey) && all.get(worldKey).isJsonArray()) {
            for (JsonElement el : all.getAsJsonArray(worldKey)) TRACKED.add(el.getAsString());
        }
    }

    private static void save() {
        if (worldKey == null) return;
        JsonObject all = readAll();
        JsonArray arr = new JsonArray();
        TRACKED.forEach(arr::add);
        all.add(worldKey, arr);
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(all, writer);
            }
        } catch (Exception e) {
            BteMobsMod.LOGGER.warn("No se ha podido guardar {}: {}", file(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------ Eventos (bus de Forge)

    @Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            load();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            TRACKED.clear();
            worldKey = null;
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (TOGGLE_KEY.consumeClick()) {
                boolean visible = !BteMobsClientConfig.TRACKER_VISIBLE.get();
                setVisible(visible);
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(Component.translatable(visible
                            ? "gui.bte_mobs.tracker.shown" : "gui.bte_mobs.tracker.hidden"), true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Registro (bus del mod)

    @Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_KEY);
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("quest_tracker", QuestTracker::render);
        }
    }

    // ------------------------------------------------------------------ Panel

    private static void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        boolean welcome = WelcomeClient.objectiveVisible();
        if ((TRACKED.isEmpty() && !welcome) || !BteMobsClientConfig.TRACKER_VISIBLE.get()) return;
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        RecipeCapability<?> cap = RecipeCapability.get(mc.player);
        if (cap == null) return;

        // Quests seguidas que existen en el cliente (las reclamadas se dejan de seguir)
        List<Map.Entry<Quest, QuestStateData>> entries = new ArrayList<>();
        boolean removed = false;
        for (String id : new ArrayList<>(TRACKED)) {
            Map.Entry<Quest, QuestStateData> entry = find(cap, id);
            if (entry == null) continue; // aún no ha llegado la sincronización
            if (entry.getValue().isReclaim) {
                TRACKED.remove(id);
                removed = true;
                continue;
            }
            entries.add(entry);
        }
        if (removed) save();
        if (entries.isEmpty() && !welcome) return;

        Font font = mc.font;
        float scale = BteMobsClientConfig.TRACKER_SCALE.get().floatValue();
        int x = BteMobsClientConfig.TRACKER_X.get();
        int y = BteMobsClientConfig.TRACKER_Y.get();
        // Alto disponible (en unidades del panel, ya escalado): hasta el chat de abajo a la izquierda
        int available = (int) ((screenHeight - y - BOTTOM_MARGIN) / scale);

        // Cada quest: título + detalle (tareas o "¡Lista!")
        List<Block> blocks = new ArrayList<>();
        // Objetivo de la bienvenida (no es una quest): hablar con los 4 NPCs del Lobby, siempre el primero
        if (welcome) {
            Block block = new Block();
            addWrapped(block.title, font, Component.translatable("gui.bte_mobs.welcome.objective",
                    WelcomeClient.doneCount(), WelcomeHandler.ORDER.length).withStyle(ChatFormatting.YELLOW), 0, 1);
            // En orden: hechos en verde, el que toca resaltado, los siguientes en gris oscuro
            WelcomeHandler.Step target = WelcomeClient.currentTarget();
            // Solo una ventana corta de pasos (el anterior, el actual y los dos siguientes) para no llenar el panel
            int currentIndex = java.util.Arrays.asList(WelcomeHandler.ORDER).indexOf(target);
            int from = Math.max(0, currentIndex - 1), to = Math.min(WelcomeHandler.ORDER.length - 1, currentIndex + 2);
            for (int i = from; i <= to; i++) {
                WelcomeHandler.Step step = WelcomeHandler.ORDER[i];
                boolean done = WelcomeClient.isDone(step);
                boolean now = step == target;
                Component name = step.npc != null
                        ? Component.translatable("entity.bte_mobs." + step.npc.name().toLowerCase(Locale.ROOT))
                        : Component.translatable("gui.bte_mobs.welcome.step." + step.name().toLowerCase(Locale.ROOT));
                Component line;
                if (now) {
                    line = Component.translatable(step.npc != null ? "gui.bte_mobs.welcome.objective.next"
                            : step == WelcomeHandler.Step.TELEPORT || step == WelcomeHandler.Step.HOME_RETURN ? "gui.bte_mobs.welcome.objective.use"
                            : step == WelcomeHandler.Step.HOME_ZONE ? "gui.bte_mobs.welcome.objective.visit"
                            : step == WelcomeHandler.Step.ALTAR ? "gui.bte_mobs.welcome.objective.visit_room"
                            : step == WelcomeHandler.Step.ELEVATOR ? "gui.bte_mobs.welcome.objective.go"
                            : step == WelcomeHandler.Step.PORTAL_ROOM || step == WelcomeHandler.Step.BTE_PORTAL ? "gui.bte_mobs.welcome.objective.go_down"
                            : step == WelcomeHandler.Step.BACK_TO_PORTAL_ROOM ? "gui.bte_mobs.welcome.objective.go_up"
                            : step == WelcomeHandler.Step.WAYSTONE ? "gui.bte_mobs.welcome.objective.activate"
                            : step == WelcomeHandler.Step.WAYSTONE_RENAME ? "gui.bte_mobs.welcome.objective.rename"
                            : step == WelcomeHandler.Step.EE_EYE || step == WelcomeHandler.Step.EE_RECIPES || step == WelcomeHandler.Step.MATRIX ? "gui.bte_mobs.welcome.objective.open_any"
                            : "gui.bte_mobs.welcome.objective.open", name).withStyle(ChatFormatting.WHITE);
                } else {
                    line = Component.literal(done ? "✔ " : "- ").append(name)
                            .withStyle(done ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY);
                }
                addWrapped(block.detail, font, line, 6, 1);
            }
            addWrapped(block.detail, font, Component.translatable("gui.bte_mobs.welcome.skip_hint",
                    WelcomeClient.SKIP_KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY), 6, 1);
            blocks.add(block);
        }
        for (Map.Entry<Quest, QuestStateData> entry : entries) {
            Quest quest = entry.getKey();
            QuestStateData state = entry.getValue();
            boolean ready = isReady(mc.player, state);
            ChatFormatting titleColor = ready ? ChatFormatting.GREEN
                    : quest.getPriorityQuest() == Quest.PriorityQuest.MAIN_QUEST ? ChatFormatting.YELLOW : ChatFormatting.AQUA;
            Block block = new Block();
            addWrapped(block.title, font, Component.literal(QuestTexts.title(quest)).withStyle(titleColor), 0, 1);
            if (ready) {
                addWrapped(block.detail, font, Component.translatable("gui.bte_mobs.tracker.ready",
                        Component.translatable("entity.bte_mobs." + quest.getEntityType().name().toLowerCase(Locale.ROOT)))
                        .withStyle(ChatFormatting.GREEN), 6, 2);
            } else {
                int i = 0;
                for (StatTaskData task : state.statTaskData) {
                    int count = progress(mc.player, task);
                    boolean done = count >= task.maxCount;
                    String desc = QuestTexts.task(quest, i, task.description);
                    Component text = Component.literal("- " + Math.min(count, task.maxCount) + "/" + task.maxCount + " " + desc)
                            .withStyle(done ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
                    addWrapped(block.detail, font, text, 6, 2);
                    i++;
                }
            }
            blocks.add(block);
        }

        // Si no cabe: primero se pliegan las quests de abajo (solo título), y si aún no cabe se recortan
        int header = LINE_HEIGHT;
        int collapsedFrom = blocks.size();
        while (collapsedFrom > 0 && height(blocks, collapsedFrom, blocks.size()) + header > available) collapsedFrom--;
        int shown = blocks.size();
        while (shown > 1 && height(blocks, collapsedFrom, shown) + header + LINE_HEIGHT > available) shown--;

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable("gui.bte_mobs.tracker.title", entries.size(), max())
                .withStyle(ChatFormatting.GOLD).getVisualOrderText(), 0));
        for (int b = 0; b < shown; b++) {
            Block block = blocks.get(b);
            lines.add(GAP);
            lines.addAll(block.title);
            if (b < collapsedFrom) lines.addAll(block.detail);
        }
        if (shown < blocks.size()) {
            lines.add(new Line(Component.translatable("gui.bte_mobs.tracker.more", blocks.size() - shown)
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText(), 0));
        }

        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0F);
        int height = 0;
        for (Line line : lines) height += line.height();
        int bg = mc.options.getBackgroundColor(0.35F);
        GuiComponent.fill(poseStack, -3, -3, PANEL_WIDTH + 3, height + 1, bg);
        int lineY = 0;
        for (Line line : lines) {
            if (line.seq() != null) font.drawShadow(poseStack, line.seq(), line.indent(), lineY, 0xFFFFFF);
            lineY += line.height();
        }
        // Tecla para ocultar el panel, a la derecha de la cabecera (si cabe y la tecla está asignada)
        if (!TOGGLE_KEY.isUnbound()) {
            Component hint = Component.translatable("gui.bte_mobs.tracker.hide_hint", TOGGLE_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.DARK_GRAY);
            int titleWidth = font.width(lines.get(0).seq());
            int hintWidth = font.width(hint);
            int room = PANEL_WIDTH - titleWidth - 8;
            float hintScale = Math.min(1.0F, room / (float) hintWidth);
            if (hintScale >= 0.6F) {
                // Si no cabe entera se dibuja algo más pequeña, alineada a la derecha y centrada en la línea
                poseStack.pushPose();
                poseStack.translate(PANEL_WIDTH - hintWidth * hintScale, (1.0F - hintScale) * 4.0F, 0);
                poseStack.scale(hintScale, hintScale, 1.0F);
                font.drawShadow(poseStack, hint, 0, 0, 0xFFFFFF);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    private static final int BOTTOM_MARGIN = 60;
    private static final Line GAP = new Line(null, 0, 4);

    private record Line(FormattedCharSequence seq, int indent, int height) {
        Line(FormattedCharSequence seq, int indent) {
            this(seq, indent, LINE_HEIGHT);
        }
    }

    private static final class Block {
        final List<Line> title = new ArrayList<>();
        final List<Line> detail = new ArrayList<>();
    }

    /** Alto de las primeras "shown" quests, con las que van desde "collapsedFrom" plegadas (solo título). */
    private static int height(List<Block> blocks, int collapsedFrom, int shown) {
        int h = 0;
        for (int b = 0; b < shown; b++) {
            Block block = blocks.get(b);
            h += GAP.height();
            for (Line l : block.title) h += l.height();
            if (b < collapsedFrom) for (Line l : block.detail) h += l.height();
        }
        return h;
    }

    /** Añade el texto partido al ancho del panel, como mucho maxLines líneas (la última acaba en "…"). */
    private static void addWrapped(List<Line> lines, Font font, Component text, int indent, int maxLines) {
        int width = PANEL_WIDTH - indent;
        List<FormattedCharSequence> split = font.split((FormattedText) text, width);
        if (split.size() <= maxLines) {
            for (FormattedCharSequence seq : split) lines.add(new Line(seq, indent));
            return;
        }
        // No cabe: se parte a mano por palabras y la última línea se recorta con "…"
        net.minecraft.network.chat.Style style = text.getStyle();
        String rest = text.getString();
        for (int i = 0; i < maxLines - 1 && !rest.isEmpty(); i++) {
            String piece = font.plainSubstrByWidth(rest, width);
            int space = piece.lastIndexOf(' ');
            if (piece.length() < rest.length() && space > 0) piece = piece.substring(0, space);
            lines.add(new Line(Component.literal(piece).withStyle(style).getVisualOrderText(), indent));
            rest = rest.substring(piece.length()).stripLeading();
        }
        String last = font.width(rest) <= width ? rest : font.plainSubstrByWidth(rest, width - font.width("…")).stripTrailing() + "…";
        lines.add(new Line(Component.literal(last).withStyle(style).getVisualOrderText(), indent));
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<Quest, QuestStateData> find(RecipeCapability<?> cap, String id) {
        for (BteNpcType npc : BteNpcType.values()) {
            Map<Quest, QuestStateData> quests = cap.getQuestForNpc(npc);
            if (quests == null) continue;
            for (Map.Entry<Quest, QuestStateData> entry : quests.entrySet()) {
                if (entry.getKey().id.toString().equals(id)) return entry;
            }
        }
        return null;
    }

    /** Progreso de una tarea. COLLECT se cuenta con el inventario del cliente (el servidor solo lo recalcula a veces). */
    private static int progress(Player player, StatTaskData task) {
        if (task.taskType == TaskData.Type.COLLECT) {
            int n = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (QuestTriggers.matchesItem(task.id, stack)) n += stack.getCount();
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (QuestTriggers.matchesItem(task.id, stack)) n += stack.getCount();
            }
            return n;
        }
        return task.complete ? task.maxCount : task.count;
    }

    private static boolean isReady(Player player, QuestStateData state) {
        if (state.isComplete) return true;
        if (state.statTaskData.isEmpty()) return false;
        for (StatTaskData task : state.statTaskData) {
            if (progress(player, task) < task.maxCount) return false;
        }
        return true;
    }
}
