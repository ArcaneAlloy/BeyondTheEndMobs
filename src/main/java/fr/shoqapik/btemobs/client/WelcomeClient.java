package fr.shoqapik.btemobs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.client.gui.QuestLogScreen;
import fr.shoqapik.btemobs.packets.WelcomeStepPacket;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import fr.shoqapik.btemobs.client.gui.QuestReadyToast;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.WelcomePacket;
import fr.shoqapik.btemobs.quest.WelcomeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Cliente de la bienvenida (ver WelcomeHandler): títulos en el centro de la pantalla, "!" sobre los NPCs con los que
 * aún no se ha hablado y el objetivo "Habla con los habitantes del Lobby" en el panel de seguimiento.
 */
public final class WelcomeClient {

    private WelcomeClient() {}

    /** Ojo del botón de Ender Eyes GUI (el del inventario), para el aviso final. */
    /** Icono del botón "Teleport to The Forgotten Realm" (Forgotten Tome Menu), sin el fondo del botón. */
    private static final ResourceLocation FORGOTTEN_REALM_ICON =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/quest_log/forgotten_realm.png");
    private static final ResourceLocation ENDER_EYES_ICON =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/quest_log/ender_eyes.png");

    /** Títulos de la bienvenida: gui.bte_mobs.welcome.<clave>.title / .subtitle */
    private static final String[] INTRO_CARDS = {"1", "2", "3"};
    /** Título que explica el Obelisco de Experiencia (al llegar a ese paso). */
    private static final String[] OBELISK_CARDS = {"obelisk"};
    /** Título que explica el botón "Teleport to The Forgotten Realm". */
    private static final String[] TELEPORT_CARDS = {"teleport"};
    /** Título que explica las waystones. */
    private static final String[] WAYSTONE_CARDS = {"waystone"};
    /**
     * Guías ilustradas que salen al terminar de hablar con cada NPC, antes de mandarle al siguiente.
     * Cada tarjeta lleva encima la imagen textures/gui/guide/<clave>.png (ancho y alto del PNG en CARD_IMAGES).
     */
    private static final String[] ANNA_GUIDE = {"anna_1", "anna_2", "anna_3", "anna_4", "anna_5", "anna_6", "anna_7"};
    private static final String[] ANTONIO_GUIDE = {"antonio_1", "antonio_4", "antonio_5", "antonio_6", "antonio_2", "antonio_3"};
    private static final String[] NOAH_GUIDE = {"noah_1", "noah_2", "noah_3", "noah_4",
            "noah_5", "noah_6", "noah_7", "noah_10", "noah_8", "noah_9"};
    private static final String[] ORIANA_GUIDE = {"oriana_1", "oriana_2", "oriana_3", "oriana_4", "oriana_5", "oriana_6"};
    private static final java.util.Map<String, int[]> CARD_IMAGES = java.util.Map.ofEntries(
            java.util.Map.entry("anna_1", new int[]{224, 224}),
            java.util.Map.entry("anna_2", new int[]{640, 128}),
            java.util.Map.entry("anna_3", new int[]{640, 157}),
            java.util.Map.entry("anna_4", new int[]{480, 454}),
            java.util.Map.entry("anna_5", new int[]{560, 408}),
            java.util.Map.entry("anna_6", new int[]{640, 328}),
            java.util.Map.entry("anna_7", new int[]{480, 453}),
            java.util.Map.entry("antonio_1", new int[]{224, 224}),
            java.util.Map.entry("antonio_2", new int[]{640, 322}),
            java.util.Map.entry("antonio_3", new int[]{640, 308}),
            java.util.Map.entry("antonio_4", new int[]{640, 339}),
            java.util.Map.entry("antonio_5", new int[]{640, 320}),
            java.util.Map.entry("antonio_6", new int[]{453, 493}),
            java.util.Map.entry("noah_1", new int[]{224, 224}),
            java.util.Map.entry("noah_2", new int[]{560, 290}),
            java.util.Map.entry("noah_3", new int[]{640, 152}),
            java.util.Map.entry("noah_4", new int[]{640, 320}),
            java.util.Map.entry("noah_5", new int[]{640, 368}),
            java.util.Map.entry("noah_6", new int[]{597, 372}),
            java.util.Map.entry("noah_7", new int[]{640, 209}),
            java.util.Map.entry("noah_8", new int[]{640, 321}),
            java.util.Map.entry("noah_9", new int[]{640, 313}),
            java.util.Map.entry("noah_10", new int[]{640, 229}),
            java.util.Map.entry("oriana_1", new int[]{224, 224}),
            java.util.Map.entry("oriana_2", new int[]{640, 309}),
            java.util.Map.entry("oriana_3", new int[]{640, 322}),
            java.util.Map.entry("oriana_4", new int[]{480, 546}),
            java.util.Map.entry("oriana_5", new int[]{640, 297}),
            java.util.Map.entry("oriana_6", new int[]{640, 323}));
    /** Al acabar la guía del NPC se anuncia el paso siguiente (aviso, haz y "!"). */
    private static boolean announceAfterCards = false;
    /** Al acabar los títulos del último paso sale el aviso de tutorial terminado. */
    private static boolean doneAfterCards = false;
    /** Se están mostrando los títulos de bienvenida: al acabar se avisa al servidor para que no vuelvan a salir. */
    private static boolean introSequence = false;
    /** Al recordar el paso actual (volver a entrar al mundo) solo sale el aviso, sin títulos. */
    private static boolean suppressCards = false;
    /** Último sitio marcado con el haz (y cuándo): el waypoint de Xaero se pone ahí. */
    static Vec3 beamTarget = null;
    static long beamTargetTime = -1000L;
    /** Títulos que se están mostrando, ticks de espera antes del primero y si ocultan el haz mientras salen. */
    private static String[] cards = INTRO_CARDS;
    private static int cardsDelay = 50;
    private static boolean cardsHideTarget = true;
    /** Ticks antes del primer título (tras cargar el mundo), y duración de cada uno. */
    private static final int START_DELAY = 50;
    private static final int FADE_IN = 12, FADE_OUT = 10, GAP = 6;
    /** Ticks mínimos que se ve un título antes de poder pasarlo (evita saltarlo sin querer). */
    private static final int MIN_STAY = 20;
    /** Título que se está mostrando, fase (0 espera inicial, 1 aparece, 2 espera la tecla, 3 desaparece) y ticks en ella. */
    private static int cardIndex = 0, cardPhase = 0, phaseTick = 0;

    /**
     * Los títulos se pasan con el clic de atacar (clic izquierdo por defecto; Enter abre el chat en el modpack).
     * Mientras se ve un título ese clic no ataca ni rompe bloques. Clics pendientes de procesar en el tick.
     */
    private static boolean continueClicked = false;

    /** Tecla para saltar el tutorial manteniéndola (Retroceso por defecto). */
    public static final net.minecraft.client.KeyMapping SKIP_KEY = new net.minecraft.client.KeyMapping(
            "key.bte_mobs.welcome_skip", net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE, "key.categories.bte_mobs");
    /** Ticks que hay que mantener la tecla para saltar el tutorial, y los que lleva. */
    private static final int SKIP_HOLD_TICKS = 60;
    private static int skipHeld = 0;

    /** Objetivo activo (hasta hablar con los 4). */
    private static boolean active = false;
    /** Pasos ya hechos (bits de WelcomeHandler.Step). */
    private static int talked = 0;
    /** Ticks de la secuencia de títulos (-1 = no hay títulos pendientes). */
    private static int introTick = -1;

    /**
     * Paso completado a la espera de mostrarse: el aviso "Siguiente" (o el final) y el cambio del haz de luz al
     * siguiente NPC salen cuando el jugador termina de hablar con el NPC (al cerrar su diálogo), no al hacer clic.
     */
    private static boolean revealPending = false;
    private static int pendingTalked = 0;
    private static boolean pendingDone = false;
    /** Se ha abierto alguna pantalla (el diálogo del NPC) desde que se completó el paso. */
    private static boolean sawScreen = false;
    private static int revealWait = 0;
    /** Si el NPC no abre ningún diálogo en este tiempo, el paso se muestra igualmente. */
    private static final int REVEAL_TIMEOUT = 40;

    public static void handle(WelcomePacket msg) {
        boolean advanced = active && !msg.intro && (msg.talked & ~talked) != 0;
        // Pasos dentro del menú de Ojos de Ender: se siguen sin cerrarlo (la guía sale encima del propio menú)
        if (advanced && !msg.done && isInGuiStep(WelcomeHandler.current(msg.talked))) {
            talked = msg.talked;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.7F));
            return;
        }
        if (advanced || (active && msg.done)) {
            // Se guarda y se muestra al cerrar el diálogo con el NPC (ver tickReveal)
            revealPending = true;
            pendingTalked = msg.talked;
            pendingDone = msg.done;
            sawScreen = false;
            revealWait = 0;
            return;
        }
        talked = msg.talked;
        if (msg.done) {
            showDone();
            return;
        }
        active = true;
        revealPending = false;
        if (msg.intro) {
            startCards(INTRO_CARDS, START_DELAY, true);
            introSequence = true;
        } else {
            announceNext(Minecraft.getInstance(), false);
        }
    }

    /** Argumentos de los subtítulos por título (p. ej. las teclas del ascensor). */
    private static final java.util.Map<String, Object[]> CARD_ARGS = new java.util.HashMap<>();
    /** Títulos del paso recién completado: salen delante de los del paso siguiente. */
    private static String[] cardPrefix = null;

    private static void startCards(String[] keys, int delay, boolean hideTarget) {
        if (suppressCards) return;
        if (cardPrefix != null) {
            String[] all = new String[cardPrefix.length + keys.length];
            System.arraycopy(cardPrefix, 0, all, 0, cardPrefix.length);
            System.arraycopy(keys, 0, all, cardPrefix.length, keys.length);
            keys = all;
            delay = Math.min(delay, 10);
            cardPrefix = null;
        }
        if (keys.length == 0) return;
        cards = keys;
        cardsDelay = delay;
        cardsHideTarget = hideTarget;
        introTick = 0;
        cardIndex = 0;
        cardPhase = 0;
        phaseTick = 0;
    }

    /**
     * Waypoint temporal de Xaero's Minimap / World Map en el sitio al que manda el haz, con el nombre del sitio.
     * Se quita en cuanto no hay haz (paso dentro de un menú, títulos en pantalla, tutorial terminado...).
     */
    private static void tickWaypoint(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.player.tickCount % 10 != 0) return;
        WelcomeHandler.Step target = active ? currentTarget() : null;
        if (target == null || beamTarget == null || mc.level.getGameTime() - beamTargetTime > 40) {
            XaeroWaypoints.clear();
            return;
        }
        Component name = target.npc != null
                ? Component.translatable("entity.bte_mobs." + target.npc.name().toLowerCase(java.util.Locale.ROOT))
                : Component.translatable("gui.bte_mobs.welcome.step." + target.name().toLowerCase(java.util.Locale.ROOT));
        XaeroWaypoints.show(name.getString(), Mth.floor(beamTarget.x), Mth.floor(beamTarget.y), Mth.floor(beamTarget.z),
                target.npc != null ? "AQUA" : "GOLD");
    }

    /** Paso del Summoning Altar: cuenta al acercarse al altar (entrar en su sala). */
    private static void tickAltar(Minecraft mc) {
        WelcomeHandler.Step target = currentTarget();
        boolean altar = target == WelcomeHandler.Step.ALTAR, storageRoom = target == WelcomeHandler.Step.STORAGE_ROOM;
        if ((!altar && !storageRoom) || revealPending) {
            altarSent = false;
            return;
        }
        if (altarSent || mc.level == null || mc.player == null || mc.player.tickCount % 10 != 0) return;
        // Storage Room: cuenta al entrar en la sala (cerca del Crafting Terminal)
        BlockPos pos = altar
                ? findNearestBlockEntity(mc.level, mc.level.getGameTime(), ALTAR_KEY, WelcomeHandler::isAltar)
                : findTerminal(mc.level, mc.level.getGameTime());
        if (pos == null) return;
        double radiusSqr = altar ? ALTAR_ROOM_RADIUS_SQR : STORAGE_ROOM_RADIUS_SQR;
        if (mc.player.position().distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) <= radiusSqr) {
            altarSent = true;
            BteMobsMod.sendToServer(new WelcomeStepPacket(target));
        }
    }

    private static void tickReveal(Minecraft mc) {
        if (!revealPending || mc.player == null) return;
        if (mc.screen != null) {
            sawScreen = true;
            return;
        }
        if (!sawScreen && ++revealWait < REVEAL_TIMEOUT) return;
        revealPending = false;
        int justDone = pendingTalked & ~talked;
        talked = pendingTalked;
        // Títulos que explican el sitio al que acaba de llegar (salen antes que los del paso siguiente)
        cardPrefix = completionCards(justDone);
        if (pendingDone) {
            if (cardPrefix != null) {
                // Último paso: primero sus títulos (el portal Beyond The End y "Todo listo") y al pasarlos, el aviso final
                active = false;
                startCards(new String[0], 0, false);
                doneAfterCards = true;
                return;
            }
            showDone();
            return;
        }
        // Guía del NPC con el que acaba de hablar: primero se explica qué hace, luego se le manda al siguiente
        String[] guide = guideCards(justDone);
        if (guide != null) {
            startCards(guide, 10, true);
            announceAfterCards = true;
            return;
        }
        announceNext(mc);
    }

    /** Guía ilustrada del NPC con el que se acaba de hablar, o null. */
    private static String[] guideCards(int justDone) {
        if ((justDone & WelcomeHandler.Step.BLACKSMITH.bit()) != 0) return ANNA_GUIDE;
        if ((justDone & WelcomeHandler.Step.EXPLORER.bit()) != 0) return ANTONIO_GUIDE;
        if ((justDone & WelcomeHandler.Step.WARLOCK.bit()) != 0) return NOAH_GUIDE;
        if ((justDone & WelcomeHandler.Step.DRUID.bit()) != 0) return ORIANA_GUIDE;
        return null;
    }

    /** Aviso del paso siguiente (toast y sus títulos); el haz y el "!" salen solos con el objetivo actual. */
    private static void announceNext(Minecraft mc) {
        announceNext(mc, true);
    }

    /**
     * Aviso del paso siguiente. Se queda fijo arriba a la derecha hasta que el jugador completa ese paso.
     * Sin títulos (withCards = false) se usa al volver a entrar al mundo, para recordar a dónde ir.
     */
    private static void announceNext(Minecraft mc, boolean withCards) {
        suppressCards = !withCards;
        try {
            announceNextStep(mc);
        } finally {
            suppressCards = false;
        }
    }

    /** El aviso se queda mientras ese paso siga siendo el actual (y el tutorial esté activo). */
    private static void addStepToast(WelcomeHandler.Step step, QuestReadyToast toast) {
        // No va a la esquina de los avisos (taparía el minimapa): se dibuja arriba en el centro (drawStepHint)
        stepHint = toast;
        stepHintStep = step;
        // Entra (con su sonido) cuando ya no quedan títulos por mostrar: primero el texto del centro, luego el aviso
        stepHintSince = -1L;
    }

    /** Aviso del paso actual, fijo arriba en el centro de la pantalla hasta que se completa el paso. */
    private static QuestReadyToast stepHint = null;
    private static WelcomeHandler.Step stepHintStep = null;
    private static long stepHintSince = 0L;

    private static void drawStepHint(PoseStack poseStack, int screenWidth) {
        if (stepHint == null) return;
        if (!active || revealPending || WelcomeHandler.current(talked) != stepHintStep) {
            stepHint = null;
            return;
        }
        // Mientras haya títulos (en pantalla o a punto de salir) no se ve; al acabar entra deslizándose
        if (introTick >= 0) {
            stepHintSince = -1L;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (stepHintSince < 0) {
            stepHintSince = net.minecraft.Util.getMillis();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F));
        }
        long time = net.minecraft.Util.getMillis() - stepHintSince;
        float slide = Mth.clamp(time / 300.0F, 0.0F, 1.0F);
        int w = stepHint.width(), h = stepHint.height();
        float x = (screenWidth - w) / 2.0F, y = -h + (h + 4) * slide;
        // Como hace ToastComponent: solo se mueve la matriz de vista del modelo, que afecta a todo (fondo, texto e
        // iconos de objeto). Si además se moviera poseStack, el fondo y el texto se desplazarían el doble.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(x, y, 0);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        stepHint.render(poseStack, mc.getToasts(), time);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void announceNextStep(Minecraft mc) {
        WelcomeHandler.Step next = WelcomeHandler.current(talked);
        if (next == null) return;
        if (next.npc != null) {
            addStepToast(next, QuestReadyToast.withFace(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.desc", npcName(next.npc)),
                    next.npc));
        } else if (next == WelcomeHandler.Step.OBELISK) {
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.obelisk"),
                    obeliskIcon()));
            // Explicación breve en el centro de la pantalla (el haz sigue visible mientras sale)
            startCards(OBELISK_CARDS, 10, false);
        } else if (next == WelcomeHandler.Step.ENDER_EYES) {
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes", key),
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes.desc"),
                    ENDER_EYES_ICON));
        } else if (next == WelcomeHandler.Step.TELEPORT) {
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.teleport"),
                    FORGOTTEN_REALM_ICON));
            // Explicación breve en el centro de la pantalla
            startCards(TELEPORT_CARDS, 10, false);
        } else if (next == WelcomeHandler.Step.WAYSTONE) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("waystones", "waystone"));
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.waystone"),
                    item != null && item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.LODESTONE)));
            startCards(WAYSTONE_CARDS, 30, false);
        } else if (next == WelcomeHandler.Step.WAYSTONE_RENAME) {
            Component sneak = mc.options.keyShift.getTranslatedKeyMessage();
            Component use = mc.options.keyUse.getTranslatedKeyMessage();
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("waystones", "waystone"));
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.waystone_rename"),
                    Component.translatable("toast.bte_mobs.welcome_next.waystone_rename.desc", sneak, use),
                    item != null && item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.NAME_TAG)));
            CARD_ARGS.put("waystone_rename", new Object[]{sneak, use});
            startCards(new String[]{"waystone_rename"}, 10, false);
        } else if (next == WelcomeHandler.Step.MATRIX) {
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes", key),
                    Component.translatable("toast.bte_mobs.welcome_next.matrix"),
                    ENDER_EYES_ICON));
        } else if (next == WelcomeHandler.Step.BACKPACK_EQUIP) {
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes", key),
                    Component.translatable("toast.bte_mobs.welcome_next.backpack_equip"),
                    backpackIcon()));
            CARD_ARGS.put("backpack_equip", new Object[]{key});
            startCards(new String[]{"backpack_equip"}, 10, false);
        } else if (next == WelcomeHandler.Step.BACKPACK_OPEN) {
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes", key),
                    Component.translatable("toast.bte_mobs.welcome_next.backpack_open"),
                    backpackIcon()));
            CARD_ARGS.put("backpack_open", new Object[]{backpackKey()});
            startCards(new String[]{"backpack_open"}, 10, false);
        } else if (next == WelcomeHandler.Step.HOME_ZONE) {
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.home_zone"),
                    teleporterIcon()));
            startCards(new String[]{"home_zone"}, 30, false);
        } else if (next == WelcomeHandler.Step.HOME_RETURN) {
            // Ya está en su Home Zone: se le explica qué es y cómo volver
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.home_return"),
                    FORGOTTEN_REALM_ICON));
            startCards(new String[]{"home_zone_info", "home_return"}, 10, false);
        } else if (next == WelcomeHandler.Step.STORAGE_ROOM) {
            // Primero tiene que ir a la sala (lo marcan el haz y el waypoint)
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.storage_room"),
                    new ItemStack(Items.CHEST)));
        } else if (next == WelcomeHandler.Step.STORAGE) {
            Item terminal = ForgeRegistries.ITEMS.getValue(WelcomeHandler.CRAFTING_TERMINAL_ID);
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.storage"),
                    terminal != null && terminal != Items.AIR ? new ItemStack(terminal) : new ItemStack(Items.CHEST)));
        } else if (next == WelcomeHandler.Step.FURNACE) {
            Item furnace = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jumbofurnace", "jumbo_furnace"));
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.furnace"),
                    furnace != null && furnace != Items.AIR ? new ItemStack(furnace) : new ItemStack(Items.FURNACE)));
            startCards(new String[]{"furnace"}, 10, false);
        } else if (next == WelcomeHandler.Step.DIM_STORAGE) {
            Item actuator = ForgeRegistries.ITEMS.getValue(WelcomeHandler.DIM_STORAGE_ID);
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.dim_storage"),
                    actuator != null && actuator != Items.AIR ? new ItemStack(actuator) : new ItemStack(Items.ENDER_CHEST)));
            startCards(new String[]{"dim_storage"}, 10, false);
        } else if (next == WelcomeHandler.Step.DIM_STORAGE_BUTTON) {
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.ender_eyes", key),
                    Component.translatable("toast.bte_mobs.welcome_next.dim_storage_button"),
                    new ItemStack(Items.ENDER_CHEST)));
            startCards(new String[]{"dim_storage_button"}, 10, false);
        } else if (next == WelcomeHandler.Step.ALTAR) {
            Item altar = ForgeRegistries.ITEMS.getValue(new ResourceLocation("summoningrituals", "altar"));
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.altar"),
                    altar != null && altar != Items.AIR ? new ItemStack(altar) : new ItemStack(Items.ENCHANTING_TABLE)));
        } else if (next == WelcomeHandler.Step.ELEVATOR) {
            // Dónde está el ascensor (lo marca el haz) y cómo se usa, con las teclas que tenga asignadas
            Component down = mc.options.keyShift.getTranslatedKeyMessage();
            Component up = mc.options.keyJump.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.elevator"),
                    new ItemStack(Items.ENDER_PEARL)));
            CARD_ARGS.put("elevator", new Object[]{down, up});
            startCards(new String[]{"elevator"}, 10, false);
        } else if (next == WelcomeHandler.Step.PORTAL_ROOM) {
            Component down = mc.options.keyShift.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.elevator_down", down),
                    Component.translatable("toast.bte_mobs.welcome_next.portal_room"),
                    new ItemStack(Items.ENDER_PEARL)));
        } else if (next == WelcomeHandler.Step.BACK_TO_PORTAL_ROOM) {
            // Desde el portal Beyond The End: volver a subir a la Portal Room
            Component up = mc.options.keyJump.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.portal_noah", up),
                    Component.translatable("toast.bte_mobs.welcome_next.back_portal_room"),
                    new ItemStack(Items.ENDER_PEARL)));
        } else if (next == WelcomeHandler.Step.PORTAL_NOAH) {
            // Última: hablar con el Noah de la Portal Room
            addStepToast(next, QuestReadyToast.withFace(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.portal_noah.desc"),
                    BteNpcType.NPC5));
        } else if (next == WelcomeHandler.Step.BTE_PORTAL) {
            Component down = mc.options.keyShift.getTranslatedKeyMessage();
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next.elevator_down", down),
                    Component.translatable("toast.bte_mobs.welcome_next.bte_portal"),
                    new ItemStack(Items.END_PORTAL_FRAME)));
        } else if (next == WelcomeHandler.Step.QUEST_LOG) {
            addStepToast(next, new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_next"),
                    Component.translatable("toast.bte_mobs.welcome_next.quest_log"),
                    new ItemStack(Items.WRITABLE_BOOK)));
        }
        // Si el paso siguiente no tenía títulos propios, salen solo los del paso completado
        startCards(new String[0], 0, false);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.7F));
    }

    /** Títulos al completar un paso (explican el sitio al que se acaba de llegar), o null. */
    private static String[] completionCards(int justDone) {
        if ((justDone & WelcomeHandler.Step.STORAGE_ROOM.bit()) != 0) return new String[]{"storage"};
        if ((justDone & WelcomeHandler.Step.BACKPACK_SKINS.bit()) != 0) return new String[]{"backpack_skins_info"};
        if ((justDone & WelcomeHandler.Step.ALTAR.bit()) != 0) return new String[]{"altar", "altar_apples"};
        if ((justDone & WelcomeHandler.Step.PORTAL_ROOM.bit()) != 0) return new String[]{"portal_room", "bte_portal", "all_set"};
        if ((justDone & WelcomeHandler.Step.PORTAL_NOAH.bit()) != 0) return new String[]{"bte_portal", "all_set"};
        return null;
    }

    /** Icono del teletransportador de Simple Teleporters (el que lleva a la Home Zone). */
    private static ItemStack teleporterIcon() {
        Item item = ForgeRegistries.ITEMS.getValue(WelcomeHandler.HOME_TELEPORTER_ID);
        return item != null && item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.GRASS_BLOCK);
    }

    /** Icono de la mochila de Backpacked (o un cofre si no está el mod). */
    private static ItemStack backpackIcon() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("backpacked", "backpack"));
        return item != null && item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.CHEST);
    }

    /** Tecla de Backpacked para abrir la mochila ("key.backpack"), para nombrarla en el texto. */
    private static Component backpackKey() {
        for (net.minecraft.client.KeyMapping key : Minecraft.getInstance().options.keyMappings) {
            if ("key.backpack".equals(key.getName())) return key.getTranslatedKeyMessage();
        }
        return Component.literal("B");
    }

    /** Icono del Obelisco de Experiencia: el bloque del mod no se ve bien en pequeño, así que una botella de experiencia. */
    private static ItemStack obeliskIcon() {
        return new ItemStack(Items.EXPERIENCE_BOTTLE);
    }

    /** Salta el tutorial: se avisa al servidor (lo da por terminado) y se quita todo lo de la bienvenida. */
    private static void skipTutorial(Minecraft mc) {
        BteMobsMod.sendToServer(new fr.shoqapik.btemobs.packets.WelcomeSkipPacket());
        active = false;
        introTick = -1;
        revealPending = false;
        cardPrefix = null;
        announceAfterCards = false;
        doneAfterCards = false;
        introSequence = false;
        mc.player.displayClientMessage(Component.empty(), true);
        mc.getToasts().addToast(new QuestReadyToast(
                Component.translatable("toast.bte_mobs.welcome_skipped"),
                Component.translatable("toast.bte_mobs.welcome_skipped.desc"),
                ENDER_EYES_ICON));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.6F));
    }

    private static void showDone() {
        active = false;
        // No se cortan los títulos que estén saliendo (p. ej. la explicación del Summoning Altar)
        Minecraft mc = Minecraft.getInstance();
        Component key = mc.options.keyInventory.getTranslatedKeyMessage();
        mc.getToasts().addToast(new QuestReadyToast(
                Component.translatable("toast.bte_mobs.welcome_done", key),
                Component.translatable("toast.bte_mobs.welcome_done.desc"),
                ENDER_EYES_ICON));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.6F));
    }

    /** Pantalla y botón del inventario de Ender Eyes GUI (por nombre: no hay dependencia del mod). */
    private static final String ENDER_EYES_SCREEN = "eu.asangarin.endereyesgui.client.screen.EnderEyesScreen";
    private static final String ENDER_EYES_BUTTON = "eu.asangarin.endereyesgui.client.screen.widget.EnderEyeInventoryButton";
    /** Pantalla del Dimensional Storage (mod occbutton) y posición de su botón respecto al inventario (StorageButtonOverlay). */
    private static final String DIM_STORAGE_SCREEN = "fr.shoqapik.occbutton.client.OccButtonStorageGui";
    private static final int DIM_STORAGE_BTN_X = -24, DIM_STORAGE_BTN_Y = 82, DIM_STORAGE_BTN_SIZE = 20;

    /** Pantallas de Backpacked, de Curios y botón de la mochila del addon btebackpackbutton (por nombre). */
    private static final String BACKPACK_SCREEN = "com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen";
    private static final String BACKPACK_CUSTOMISE_SCREEN = "com.mrcrayfish.backpacked.client.gui.screen.CustomiseBackpackScreen";
    private static final String CURIOS_SCREEN = "top.theillusivec4.curios.client.gui.CuriosScreen";
    private static final String BACKPACK_BUTTON = "com.arcanealloy.btebackpackbutton.client.BackpackInventoryButton";

    /** Botón "Teleport to The Forgotten Realm" de Forgotten Tome Menu (por reflexión: no hay dependencia del mod). */
    private static java.lang.reflect.Field tomeButtonField;
    private static boolean tomeButtonLookedUp = false;

    private static Object forgottenTomeButton() {
        if (!tomeButtonLookedUp) {
            tomeButtonLookedUp = true;
            try {
                tomeButtonField = Class.forName("com.binaris.forgotten_tome_menu.ForgottenButtonHandler").getField("bookButton");
            } catch (ReflectiveOperationException | LinkageError e) {
                tomeButtonField = null;
            }
        }
        if (tomeButtonField == null) return null;
        try {
            return tomeButtonField.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ Menú de Ojos de Ender (Ender Eyes GUI, por reflexión)

    /** Botones de los menús de recetas del menú de Ojos de Ender. */
    /** Los 4 menús de recetas en el orden de la fila de abajo: clave del texto y botón que lo abre. */
    private static final String[] EE_RECIPE_MENUS = {"ee_recipes_nature", "ee_recipes_enchanting", "ee_recipes_blacksmith", "ee_recipes_potions"};
    private static final String[] EE_RECIPE_MENU_BUTTONS = {"NatureButton", "EnchantmentBookButton", "BlacksmithButton", "PotionButton"};
    /** Menús de recetas que ya ha abierto en el paso de las recetas (hay que entrar en los 4). */
    private static final java.util.Set<String> EE_RECIPES_SEEN = new java.util.HashSet<>();

    /** Siguiente menú de recetas que le falta por abrir (índice), o -1 si ya los ha visto todos. */
    private static int nextRecipeMenu() {
        for (int i = 0; i < EE_RECIPE_MENUS.length; i++) if (!EE_RECIPES_SEEN.contains(EE_RECIPE_MENUS[i])) return i;
        return -1;
    }

    private static final java.util.Set<String> EE_RECIPE_BUTTONS = java.util.Set.of(
            "NatureButton", "EnchantmentBookButton", "BlacksmithButton", "PotionButton");
    private static final String EE_MATRIX_SCREEN = "eu.asangarin.endereyesgui.client.screen.MatrixStorageScreen";
    private static final String EE_INSPECT_SCREEN = "eu.asangarin.endereyesgui.client.screen.EnderEyeInspectScreen";
    private static final String EE_SCREEN_PACKAGE = "eu.asangarin.endereyesgui.client.screen.";
    private static java.lang.reflect.Field inspectEyeField;

    /** Ojo o dimensión que muestra la pantalla de detalle de Ender Eyes GUI (o null si no es esa pantalla). */
    private static Object inspectedEyeObject(Screen screen) {
        if (screen == null || !EE_INSPECT_SCREEN.equals(screen.getClass().getName())) return null;
        try {
            if (inspectEyeField == null) {
                inspectEyeField = screen.getClass().getDeclaredField("eye");
                inspectEyeField.setAccessible(true);
            }
            return inspectEyeField.get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Nombre (enum) del ojo o dimensión que muestra la pantalla de detalle, p. ej. "TWILIGHT_FOREST". */
    private static String inspectedEye(Screen screen) {
        Object eye = inspectedEyeObject(screen);
        return eye instanceof Enum<?> e ? e.name() : null;
    }

    /** Menús de recetas de Ender Eyes GUI (herrería, encantamientos, pociones, Druida y Explorador). */
    private static boolean isEeRecipesScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith(EE_SCREEN_PACKAGE) && name.endsWith("RecipesScreen");
    }

    /** Distancia (bloques) a la que va el haz detrás del NPC, visto desde la cámara. */
    private static final double BEAM_BEHIND = 0.9D;

    /** Color del haz de cada NPC (el mismo que en el mapa de quests). */
    private static float[] beamColor(WelcomeHandler.Step step) {
        return switch (step) {
            case BLACKSMITH -> new float[]{1.0F, 0.55F, 0.2F};   // Anna: naranja de la forja
            case EXPLORER -> new float[]{0.35F, 0.6F, 1.0F};     // Antonio: azul
            case WARLOCK -> new float[]{0.7F, 0.4F, 1.0F};       // Noah: morado
            case DRUID -> new float[]{0.4F, 1.0F, 0.45F};        // Oriana: verde
            case OBELISK -> new float[]{0.5F, 1.0F, 0.3F};       // Obelisco: verde experiencia
            case WAYSTONE, WAYSTONE_RENAME -> new float[]{0.9F, 0.5F, 1.0F}; // Waystone: rosa de sus runas
            case STORAGE, STORAGE_ROOM -> new float[]{1.0F, 0.8F, 0.35F}; // Storage Room / Crafting Terminal: dorado
            case FURNACE -> new float[]{1.0F, 0.45F, 0.15F};     // Jumbo Furnace: fuego
            case DIM_STORAGE -> new float[]{0.55F, 0.35F, 1.0F}; // Dimensional Storage: violeta
            case ALTAR -> new float[]{1.0F, 0.25F, 0.2F};        // Summoning Altar: rojo
            case HOME_ZONE -> new float[]{0.85F, 0.3F, 1.0F};    // Teletransportador: morado de sus partículas
            case ELEVATOR -> new float[]{0.4F, 0.95F, 0.9F};     // Ascensor: turquesa de sus luces
            case PORTAL_NOAH -> new float[]{0.7F, 0.4F, 1.0F};   // Noah de la Portal Room: morado, como Noah
            default -> new float[]{1.0F, 1.0F, 1.0F};
        };
    }

    private static Component npcName(BteNpcType npc) {
        return Component.translatable("entity.bte_mobs." + npc.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** Paso que toca ahora (null si no hay objetivo o aún se ven los títulos de entrada). */
    public static WelcomeHandler.Step currentTarget() {
        if (!active || (introRunning() && cardsHideTarget)) return null;
        return WelcomeHandler.current(talked);
    }

    private static boolean isInGuiStep(WelcomeHandler.Step step) {
        return step == WelcomeHandler.Step.EE_DIMENSION || step == WelcomeHandler.Step.EE_EYE
                || step == WelcomeHandler.Step.EE_RECIPES || step == WelcomeHandler.Step.BACKPACK_SKINS;
    }

    /** ¿Hay un título en pantalla (apareciendo, esperando o desapareciendo)? */
    private static boolean cardVisible() {
        return introTick >= 0 && cardPhase != 0;
    }

    /** true mientras se muestran títulos. */
    private static boolean introRunning() {
        return introTick >= 0;
    }

    /** "!" y haz de luz sobre este NPC: es el que toca. */
    public static boolean pending(BteNpcType type) {
        WelcomeHandler.Step step = currentTarget();
        return type != null && step != null && step.npc == type;
    }

    /** ¿Hay algo marcado por la bienvenida? */
    public static boolean anyPending() {
        return currentTarget() != null;
    }

    /** ¿Se muestra el objetivo en el panel de seguimiento? */
    public static boolean objectiveVisible() {
        return anyPending();
    }

    public static int doneCount() {
        return Integer.bitCount(talked & WelcomeHandler.ALL);
    }

    public static boolean isDone(WelcomeHandler.Step step) {
        return (talked & step.bit()) != 0;
    }

    /** Obelisco de Experiencia más cercano a Noah (se busca cada 2 s mientras toque ese paso). */
    private static BlockPos obeliskPos = null;
    private static long obeliskSearchTime = -1000L;
    private static final int OBELISK_SEARCH_RADIUS = 12;

    private static BlockPos findObelisk(ClientLevel level, long gameTime) {
        if (obeliskPos != null) {
            if (isObelisk(level, obeliskPos)) return obeliskPos;
            obeliskPos = null;
        }
        if (gameTime - obeliskSearchTime < 40) return null;
        obeliskSearchTime = gameTime;
        // Centro de búsqueda: Noah si está cargado, si no el jugador
        BlockPos center = null;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof BteAbstractEntity npc && npc.getNpcType() == BteNpcType.WARLOCK) {
                center = npc.blockPosition();
                break;
            }
        }
        if (center == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return null;
            center = mc.player.blockPosition();
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = OBELISK_SEARCH_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -6, -r), center.offset(r, 6, r))) {
            if (!isObelisk(level, pos)) continue;
            double d = pos.distSqr(center);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        obeliskPos = best;
        return best;
    }

    /** Waystone del Lobby: la más cercana al punto de llegada del teletransporte (0 116 0), entre las cargadas. */
    private static BlockPos waystonePos = null;
    private static long waystoneSearchTime = -1000L;
    private static final ResourceLocation LOBBY_ID = new ResourceLocation("ender_journey", "the_forgotten_realm");

    private static BlockPos findWaystone(ClientLevel level, long gameTime) {
        if (!level.dimension().location().equals(LOBBY_ID)) return null;
        if (waystonePos != null) {
            if (isWaystone(level, waystonePos)) return waystonePos;
            waystonePos = null;
        }
        if (gameTime - waystoneSearchTime < 40) return null;
        waystoneSearchTime = gameTime;
        // Se miran las block entities de los chunks cargados alrededor del punto de llegada (barato)
        BlockPos arrival = new BlockPos(0, 116, 0);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = 4;
        for (int cx = -r; cx <= r; cx++) {
            for (int cz = -r; cz <= r; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (!isWaystone(level, pos)) continue;
                    // Bloque de abajo (la waystone ocupa dos)
                    BlockPos base = isWaystone(level, pos.below()) ? pos.below() : pos;
                    double d = base.distSqr(arrival);
                    if (d < bestDist) {
                        bestDist = d;
                        best = base.immutable();
                    }
                }
            }
        }
        waystonePos = best;
        return best;
    }

    /** Crafting Terminal de la Storage Room: el más cercano al jugador entre los chunks cargados a su alrededor. */
    private static BlockPos terminalPos = null;
    private static long terminalSearchTime = -1000L;

    private static BlockPos findTerminal(ClientLevel level, long gameTime) {
        if (!level.dimension().location().equals(LOBBY_ID)) return null;
        if (terminalPos != null) {
            if (isBlock(level, terminalPos, WelcomeHandler.CRAFTING_TERMINAL_ID)) return terminalPos;
            terminalPos = null;
        }
        if (gameTime - terminalSearchTime < 40) return null;
        terminalSearchTime = gameTime;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        BlockPos center = mc.player.blockPosition();
        int pcx = center.getX() >> 4, pcz = center.getZ() >> 4, r = 5;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (!isBlock(level, pos, WelcomeHandler.CRAFTING_TERMINAL_ID)) continue;
                    double d = pos.distSqr(center);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
        }
        terminalPos = best;
        return best;
    }

    /** Block entity más cercana al jugador con ese id de bloque (chunks cargados a su alrededor), cacheada. */
    private static final java.util.Map<ResourceLocation, BlockPos> NEAREST_CACHE = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, Long> NEAREST_TIME = new java.util.HashMap<>();

    private static BlockPos findNearestBlockEntity(ClientLevel level, long gameTime, ResourceLocation id) {
        return findNearestBlockEntity(level, gameTime, id, id::equals);
    }

    private static BlockPos findNearestBlockEntity(ClientLevel level, long gameTime, ResourceLocation id,
                                                   java.util.function.Predicate<ResourceLocation> matches) {
        BlockPos cached = NEAREST_CACHE.get(id);
        if (cached != null && matches.test(blockId(level, cached))) return cached;
        if (gameTime - NEAREST_TIME.getOrDefault(id, -1000L) < 40) return null;
        NEAREST_TIME.put(id, gameTime);
        NEAREST_CACHE.remove(id);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        BlockPos center = mc.player.blockPosition();
        int pcx = center.getX() >> 4, pcz = center.getZ() >> 4, r = 5;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (!matches.test(blockId(level, pos))) continue;
                    double d = pos.distSqr(center);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
        }
        if (best != null) NEAREST_CACHE.put(id, best);
        return best;
    }

    /** Ascensor más cercano al jugador: centro de la plataforma (todos los bloques de ascensor juntos) y su cara de arriba. */
    private static Vec3 elevatorPos = null;
    private static long elevatorSearchTime = -1000L;

    private static Vec3 findElevator(ClientLevel level, long gameTime) {
        if (gameTime - elevatorSearchTime < 40) return elevatorPos;
        elevatorSearchTime = gameTime;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        // En la planta de arriba se busca en la sala del ascensor (para que el haz se vea desde lejos); si no, junto al jugador
        BlockPos center = mc.player.getY() >= 100 ? WelcomeHandler.ELEVATOR_TOP : mc.player.blockPosition();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        int r = 16;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -6, -r), center.offset(r, 6, r))) {
            if (!WelcomeHandler.isElevator(blockId(level, pos))) continue;
            double d = pos.distSqr(center);
            if (d < bestDist) {
                bestDist = d;
                nearest = pos.immutable();
            }
        }
        if (nearest == null) {
            elevatorPos = null;
            return null;
        }
        // Toda la plataforma (bloques de ascensor a la misma altura y pegados): el haz va en su centro
        double sx = 0, sz = 0;
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(nearest.offset(-3, 0, -3), nearest.offset(3, 0, 3))) {
            if (!WelcomeHandler.isElevator(blockId(level, pos))) continue;
            sx += pos.getX() + 0.5D;
            sz += pos.getZ() + 0.5D;
            n++;
        }
        elevatorPos = new Vec3(sx / n, nearest.getY() + 1, sz / n);
        return elevatorPos;
    }

    /** Jumbo Furnace más cercano al jugador: centro (x, z) del multibloque y su base (y). */
    private static Vec3 furnacePos = null;
    private static long furnaceSearchTime = -1000L;

    private static Vec3 findFurnace(ClientLevel level, long gameTime) {
        if (furnacePos != null && gameTime - furnaceSearchTime < 100) return furnacePos;
        if (gameTime - furnaceSearchTime < 40) return furnacePos;
        furnaceSearchTime = gameTime;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        BlockPos center = mc.player.blockPosition();
        // Bloque del horno más cercano y, a partir de él, todos los del multibloque (a 2 bloques como mucho)
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        int r = 10;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -4, -r), center.offset(r, 4, r))) {
            if (!WelcomeHandler.isJumboFurnace(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()))) continue;
            double d = pos.distSqr(center);
            if (d < bestDist) {
                bestDist = d;
                nearest = pos.immutable();
            }
        }
        if (nearest == null) {
            furnacePos = null;
            return null;
        }
        double sx = 0, sz = 0;
        int n = 0, minY = Integer.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(nearest.offset(-2, -2, -2), nearest.offset(2, 2, 2))) {
            if (!WelcomeHandler.isJumboFurnace(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()))) continue;
            sx += pos.getX() + 0.5D;
            sz += pos.getZ() + 0.5D;
            minY = Math.min(minY, pos.getY());
            n++;
        }
        double cx = sx / n, cz = sz / n;
        // Cara de delante: el lado con más espacio libre delante (3 de ancho x 3 de fondo x 2 de alto), así los lados
        // pegados a pilares o paredes pierden frente a la sala; si empatan, el más cercano al jugador.
        // El haz va un bloque por delante de esa cara, centrado.
        Vec3 front = new Vec3(cx, minY, cz);
        int bestFree = -1;
        double bestPlayer = Double.MAX_VALUE;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int free = 0;
            for (int depth = 2; depth <= 4; depth++) {
                for (int side = -1; side <= 1; side++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockPos out = new BlockPos(
                                Mth.floor(cx + dir.getStepX() * depth + dir.getStepZ() * side),
                                minY + dy,
                                Mth.floor(cz + dir.getStepZ() * depth + dir.getStepX() * side));
                        if (level.getBlockState(out).getCollisionShape(level, out).isEmpty()) free++;
                    }
                }
            }
            Vec3 candidate = new Vec3(cx + dir.getStepX() * 2.0D, minY, cz + dir.getStepZ() * 2.0D);
            double toPlayer = candidate.distanceToSqr(mc.player.position());
            if (free > bestFree || (free == bestFree && toPlayer < bestPlayer)) {
                bestFree = free;
                bestPlayer = toPlayer;
                front = candidate;
            }
        }
        furnacePos = front;
        return furnacePos;
    }

    private static ResourceLocation blockId(ClientLevel level, BlockPos pos) {
        return ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
    }

    /** Clave de caché para el Summoning Altar (hay varios ids de altar). */
    private static final ResourceLocation ALTAR_KEY = new ResourceLocation("summoningrituals", "altar");
    /** Distancia al altar a la que se considera que ha entrado en su sala. */
    private static final double ALTAR_ROOM_RADIUS_SQR = 6.0D * 6.0D;
    /** Distancia al Crafting Terminal a la que se considera que ha entrado en la Storage Room. */
    private static final double STORAGE_ROOM_RADIUS_SQR = 8.0D * 8.0D;
    private static boolean altarSent = false;

    private static boolean isBlock(ClientLevel level, BlockPos pos, ResourceLocation id) {
        return id.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()));
    }

    private static boolean isWaystone(ClientLevel level, BlockPos pos) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        return id != null && id.getNamespace().equals("waystones") && id.getPath().endsWith("waystone");
    }

    private static boolean isObelisk(ClientLevel level, BlockPos pos) {
        return WelcomeHandler.OBELISK_ID.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()));
    }

    // ------------------------------------------------------------------ Eventos (bus de Forge)

    @Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class WelcomeForgeEvents {
        private WelcomeForgeEvents() {}

        @SubscribeEvent
        public static void onWelcomeClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            tickReveal(mc);
            tickAltar(mc);
            tickSkip(mc);
            tickWaypoint(mc);
            if (introTick < 0) return;
            // Se espera a que el mundo esté cargado y no haya pantallas abiertas (carga, menú de pausa...)
            if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()) {
                continueClicked = false; // no se pasan títulos que no se ven
                return;
            }
            introTick++;
            phaseTick++;
            boolean pressed = continueClicked;
            continueClicked = false;
            switch (cardPhase) {
                case 0 -> { // espera inicial
                    if (phaseTick >= cardsDelay) nextPhase(mc, 1);
                }
                case 1 -> { // aparece
                    if (phaseTick >= FADE_IN) nextPhase(mc, 2);
                }
                case 2 -> { // se queda hasta que pulse la tecla de continuar
                    if (pressed && phaseTick >= MIN_STAY) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F, 0.4F));
                        nextPhase(mc, 3);
                    }
                }
                default -> { // desaparece
                    if (phaseTick >= FADE_OUT + GAP) {
                        cardIndex++;
                        if (cardIndex >= cards.length) {
                            introTick = -1;
                            // Tras los títulos de entrada aparecen el haz y el "!" sobre el primer NPC
                            if (introSequence) {
                                introSequence = false;
                                BteMobsMod.sendToServer(new fr.shoqapik.btemobs.packets.WelcomeIntroSeenPacket());
                            }
                            if (announceAfterCards) {
                                // Fin de la guía del NPC: ahora sí se le manda al siguiente
                                announceAfterCards = false;
                                if (active) announceNext(mc);
                            } else if (doneAfterCards) {
                                doneAfterCards = false;
                                showDone();
                            } else if (cardsHideTarget) {
                                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.6F));
                            }
                        } else {
                            nextPhase(mc, 1);
                        }
                    }
                }
            }
        }

        private static void nextPhase(Minecraft mc, int phase) {
            cardPhase = phase;
            phaseTick = 0;
            if (phase == 1) {
                float pitch = cardIndex == cards.length - 1 ? 1.2F : 0.9F;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 1.0F));
            }
        }

        /** Clic de atacar mientras se ve un título: pasa al siguiente (si ya se puede) y no ataca ni rompe bloques. */
        @SubscribeEvent
        public static void onWelcomeMouse(net.minecraftforge.client.event.InputEvent.MouseButton.Pre event) {
            if (!cardVisible()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || !mc.options.keyAttack.matchesMouse(event.getButton())) return;
            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) continueClicked = true;
            event.setCanceled(true);
        }

        /** Si el clic de atacar está en una tecla del teclado, también sirve. */
        @SubscribeEvent
        public static void onWelcomeKey(net.minecraftforge.client.event.InputEvent.Key event) {
            if (!cardVisible()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS) return;
            if (mc.options.keyAttack.matches(event.getKey(), event.getScanCode())) continueClicked = true;
        }

        /** Mantener la tecla de saltar (Retroceso) unos segundos salta todo el tutorial. */
        private static void tickSkip(Minecraft mc) {
            if (!active || mc.player == null || mc.screen != null) {
                skipHeld = 0;
                return;
            }
            if (!SKIP_KEY.isDown()) {
                if (skipHeld > 0) mc.player.displayClientMessage(Component.empty(), true);
                skipHeld = 0;
                return;
            }
            skipHeld++;
            int percent = Math.min(100, skipHeld * 100 / SKIP_HOLD_TICKS);
            mc.player.displayClientMessage(Component.translatable("gui.bte_mobs.welcome.skipping", percent)
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            if (skipHeld >= SKIP_HOLD_TICKS) {
                skipHeld = 0;
                skipTutorial(mc);
            }
        }

        /** Haz de luz tipo faro que sale de debajo del NPC con el que toca hablar (visible desde lejos). */
        @SubscribeEvent
        public static void onRenderWelcomeBeam(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            WelcomeHandler.Step target = currentTarget();
            if (target == null) return;
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) return;
            Vec3 cam = event.getCamera().getPosition();
            float partialTick = event.getPartialTick();
            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            boolean drawn = false;
            if (target.npc != null) {
                for (Entity entity : level.entitiesForRendering()) {
                    if (!(entity instanceof BteAbstractEntity npc) || npc.getNpcType() != target.npc || npc.isInvisible()) continue;
                    double x = Mth.lerp(partialTick, npc.xOld, npc.getX());
                    double y = Mth.lerp(partialTick, npc.yOld, npc.getY());
                    double z = Mth.lerp(partialTick, npc.zOld, npc.getZ());
                    drawBeam(level, pose, buffers, cam, partialTick, x, y, z, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.WAYSTONE || target == WelcomeHandler.Step.WAYSTONE_RENAME) {
                BlockPos pos = findWaystone(level, level.getGameTime());
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.STORAGE || target == WelcomeHandler.Step.STORAGE_ROOM) {
                BlockPos pos = findTerminal(level, level.getGameTime());
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.ELEVATOR) {
                // En medio del bloque central del ascensor (el amarillo), saliendo de su cara de arriba
                Vec3 pos = level.dimension().location().equals(LOBBY_ID) ? findElevator(level, level.getGameTime()) : null;
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.x, pos.y, pos.z, beamColor(target), 0.0D);
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.HOME_ZONE) {
                // Teletransportador a la Home Zone: el más cercano al jugador (está junto a la waystone del Lobby)
                BlockPos pos = level.dimension().location().equals(LOBBY_ID)
                        ? findNearestBlockEntity(level, level.getGameTime(), WelcomeHandler.HOME_TELEPORTER_ID) : null;
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.ALTAR) {
                BlockPos pos = findNearestBlockEntity(level, level.getGameTime(), ALTAR_KEY, WelcomeHandler::isAltar);
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.DIM_STORAGE) {
                BlockPos pos = findNearestBlockEntity(level, level.getGameTime(), WelcomeHandler.DIM_STORAGE_ID);
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.FURNACE) {
                Vec3 pos = findFurnace(level, level.getGameTime());
                if (pos != null) {
                    // Delante del horno (está metido entre bloques: detrás no se vería)
                    drawBeam(level, pose, buffers, cam, partialTick, pos.x, pos.y, pos.z, beamColor(target), 0.0D);
                    drawn = true;
                }
            } else if (target == WelcomeHandler.Step.OBELISK) {
                BlockPos pos = findObelisk(level, level.getGameTime());
                if (pos != null) {
                    drawBeam(level, pose, buffers, cam, partialTick, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, beamColor(target));
                    drawn = true;
                }
            }
            if (drawn) buffers.endBatch();
        }

        private static void drawBeam(ClientLevel level, PoseStack pose, MultiBufferSource buffers, Vec3 cam, float partialTick,
                                     double x, double y, double z, float[] color) {
            drawBeam(level, pose, buffers, cam, partialTick, x, y, z, color, BEAM_BEHIND);
        }

        /** behind: bloques que se desplaza el haz detrás de lo marcado (visto desde la cámara); 0 = justo en el sitio. */
        private static void drawBeam(ClientLevel level, PoseStack pose, MultiBufferSource buffers, Vec3 cam, float partialTick,
                                     double x, double y, double z, float[] color, double behind) {
            beamTarget = new Vec3(x, y, z);
            beamTargetTime = level.getGameTime();
            // El haz va justo detrás de lo marcado, visto desde la cámara, para que nunca lo tape
            double dx = x - cam.x, dz = z - cam.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (behind != 0.0D && len > 1.0E-3D) {
                x += dx / len * behind;
                z += dz / len * behind;
            }
            int height = Math.max(1, level.getMaxBuildHeight() - Mth.floor(y));
            pose.pushPose();
            // renderBeaconBeam centra el haz en (0.5, 0.5) del bloque: se compensa
            pose.translate(x - cam.x - 0.5D, y - cam.y, z - cam.z - 0.5D);
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                    level.getGameTime(), 0, height, color, 0.2F, 0.25F);
            pose.popPose();
        }

        /** Abrir con su botón el menú de Ojos de Ender o el libro de quests (pasos que solo ve el cliente). */
        @SubscribeEvent
        public static void onWelcomeScreenOpening(ScreenEvent.Opening event) {
            WelcomeHandler.Step target = currentTarget();
            if (target == null || !WelcomeHandler.isClientStep(target) || revealPending) return;
            Screen screen = event.getNewScreen();
            if (screen == null) return;
            boolean matches = switch (target) {
                case ENDER_EYES -> ENDER_EYES_SCREEN.equals(screen.getClass().getName());
                case EE_DIMENSION -> "TWILIGHT_FOREST".equals(inspectedEye(screen));
                case EE_EYE -> {
                    Object eye = inspectedEyeObject(screen);
                    yield eye != null && eye.getClass().getSimpleName().equals("EnderEye");
                }
                case EE_RECIPES -> {
                    String menu = isEeRecipesScreen(screen) ? eeRecipesBanner(screen) : null;
                    if (menu != null) EE_RECIPES_SEEN.add(menu);
                    yield menu != null && nextRecipeMenu() < 0;
                }
                case MATRIX -> EE_MATRIX_SCREEN.equals(screen.getClass().getName());
                // Menú de ajustes de la waystone (cambiar el nombre): mano vacía + Shift + clic derecho
                case WAYSTONE_RENAME -> screen.getClass().getName().startsWith("net.blay09.mods.waystones")
                        && screen.getClass().getSimpleName().contains("Settings");
                case QUEST_LOG -> screen instanceof QuestLogScreen;
                // La mochila (desde el botón del inventario o con su tecla) y su pantalla "Customize"
                case BACKPACK_OPEN -> BACKPACK_SCREEN.equals(screen.getClass().getName());
                case BACKPACK_SKINS -> BACKPACK_CUSTOMISE_SCREEN.equals(screen.getClass().getName());
                // Solo desde el botón del inventario (con clic derecho en el bloque se abre la misma pantalla)
                case DIM_STORAGE_BUTTON -> DIM_STORAGE_SCREEN.equals(screen.getClass().getName())
                        && event.getCurrentScreen() instanceof InventoryScreen;
                default -> false;
            };
            if (matches) BteMobsMod.sendToServer(new WelcomeStepPacket(target));
        }

        /**
         * Guía dentro de los menús (menú de Ojos de Ender y sus pantallas, libro de quests): un panel a la izquierda con
         * la explicación y lo que hay que hacer, y un marco dorado en el botón que toca.
         */
        @SubscribeEvent
        public static void onWelcomeGuiBanner(ScreenEvent.Render.Post event) {
            WelcomeHandler.Step target = currentTarget();
            if (target == null) return;
            Screen screen = event.getScreen();
            String name = screen.getClass().getName();
            String banner = null, action = null;
            /** Menú de Ojos de Ender: bloque que lleva la acción (0 dimensiones, 1 ojos, 2 recetas); -1 = no es ese menú. */
            int eeBlock = -1;
            java.util.function.Predicate<AbstractWidget> highlightWidget = null;
            int w = screen.width, h = screen.height;
            boolean recipesDone = EE_RECIPES_SEEN.size() >= EE_RECIPE_MENUS.length
                    && (target == WelcomeHandler.Step.EE_RECIPES || target == WelcomeHandler.Step.QUEST_LOG);
            if (ENDER_EYES_SCREEN.equals(name) && recipesDone) {
                // Ya ha visto los 4 menús de recetas: solo queda cerrar el menú
                banner = "ee_done";
                action = "close";
            } else if (ENDER_EYES_SCREEN.equals(name)) {
                switch (target) {
                    case EE_DIMENSION -> {
                        banner = "ee_menu";
                        eeBlock = 0;
                        action = "ee_dimension";
                        // Botón de la dimensión del Twilight Forest (fila de arriba, segunda posición)
                        highlightWidget = wd -> wd.x == w / 2 + (13 * -4 - 1) - 13 && wd.y == h / 2 + 13 * -8 - 13;
                    }
                    case ENDER_EYES, EE_EYE -> {
                        banner = "ee_menu";
                        eeBlock = 1;
                        action = "ee_eye";
                        AbstractWidget first = firstEye(screen, h / 2 + 13 * -8 - 13);
                        highlightWidget = wd -> wd == first;
                    }
                    case EE_RECIPES -> {
                        banner = "ee_menu";
                        eeBlock = 2;
                        action = "ee_recipes";
                        int next = nextRecipeMenu();
                        String nextButton = next >= 0 ? EE_RECIPE_MENU_BUTTONS[next] : null;
                        highlightWidget = wd -> wd.getClass().getSimpleName().equals(nextButton);
                    }
                    case MATRIX -> {
                        // Solo su propio texto, junto al botón del Matrix Storage
                        banner = "matrix_menu";
                        eeBlock = 3;
                        action = "matrix";
                        highlightWidget = wd -> wd.getClass().getSimpleName().equals("MatrixStorageButton");
                    }
                    default -> { }
                }
            } else if (EE_INSPECT_SCREEN.equals(name)) {
                if (target == WelcomeHandler.Step.EE_EYE) {
                    banner = "ee_dimension_info";
                    action = "ee_back_eye";
                } else if (target == WelcomeHandler.Step.EE_RECIPES) {
                    banner = "ee_eye_info";
                    action = "ee_back_recipes";
                }
            } else if (EE_MATRIX_SCREEN.equals(name) && target == WelcomeHandler.Step.MATRIX) {
                // El panel morado ocupa el centro (300 de ancho como mucho): el texto va en el hueco de la izquierda
                drawInLeftGap(event.getPoseStack(), screen, Math.min(300, w - 32), "matrix_info", "close");
                return;
            } else if (isEeRecipesScreen(screen) && target == WelcomeHandler.Step.EE_RECIPES) {
                // Cada menú de recetas tiene su explicación, dentro del panel morado de la derecha
                String key = eeRecipesBanner(screen);
                if (key != null) {
                    drawInRecipePanel(event.getPoseStack(), screen, key, nextRecipeMenu() < 0 ? "close" : "ee_recipes_back_next");
                    return;
                }
                banner = "ee_recipes_info";
                action = "close";
            } else if (target == WelcomeHandler.Step.BACKPACK_EQUIP && screen instanceof InventoryScreen) {
                banner = "backpack_equip";
                action = "backpack_curios";
            } else if (target == WelcomeHandler.Step.BACKPACK_EQUIP && CURIOS_SCREEN.equals(name)) {
                banner = "backpack_equip";
                action = "backpack_back_slot";
            } else if (target == WelcomeHandler.Step.BACKPACK_OPEN && screen instanceof InventoryScreen) {
                banner = "backpack_open";
                action = "backpack_button";
            } else if (target == WelcomeHandler.Step.BACKPACK_SKINS && BACKPACK_SCREEN.equals(name)) {
                banner = "backpack_menu";
                action = "backpack_customise";
                // El primer botón pequeño de arriba es "Customize" (el segundo, las opciones)
                AbstractWidget customise = firstWidget(screen, "MiniButton");
                highlightWidget = wd -> wd == customise;
            } else if (target == WelcomeHandler.Step.QUEST_LOG && (screen instanceof QuestLogScreen
                    || screen instanceof fr.shoqapik.btemobs.client.gui.QuestScreen)) {
                banner = "quest_log_info";
                action = "close";
            }
            if (banner == null) return;
            if (highlightWidget != null) {
                for (GuiEventListener child : screen.children()) {
                    if (child instanceof AbstractWidget widget && widget.visible && highlightWidget.test(widget)
                            && !hitsTooltip(screen, event.getMouseX(), event.getMouseY(), widget.x, widget.y, widget.getWidth(), widget.getHeight())) {
                        highlight(event.getPoseStack(), event.getPartialTick(), widget.x, widget.y, widget.getWidth(), widget.getHeight());
                    }
                }
            }
            if (eeBlock == 3) {
                drawNextToWidget(event.getPoseStack(), screen, firstWidget(screen, "MatrixStorageButton"), banner, action);
                return;
            }
            if (eeBlock >= 0) {
                drawEeMenuGuide(event.getPoseStack(), screen, eeBlock, action);
                return;
            }
            // En los menús de recetas y en los de quests va abajo a la derecha (a la izquierda tapa sus listas)
            boolean questScreen = screen instanceof QuestLogScreen || screen instanceof fr.shoqapik.btemobs.client.gui.QuestScreen;
            // La lista de NPCs (botón de quests del inventario) tiene sitio libre a la izquierda; la de quests de un NPC no
            boolean bottomRight = isEeRecipesScreen(screen) || screen instanceof fr.shoqapik.btemobs.client.gui.QuestScreen;
            drawBanner(event.getPoseStack(), screen, banner, action, bottomRight, questScreen ? 180 : 220);
        }

        /** ¿Está el ratón encima de algún botón (y por tanto puede verse su tooltip)? */
        private static boolean mouseOverWidget(Screen screen, double mouseX, double mouseY) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget wd && wd.visible && wd.isMouseOver(mouseX, mouseY)) return true;
            }
            return false;
        }

        /** Primer widget de la pantalla con ese nombre de clase (en el orden en que se añadieron). */
        private static AbstractWidget firstWidget(Screen screen, String simpleName) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget wd && wd.getClass().getSimpleName().equals(simpleName)) return wd;
            }
            return null;
        }

        /** Primer Ojo de Ender del menú (el de más arriba a la izquierda, debajo de la fila de dimensiones). */
        private static AbstractWidget firstEye(Screen screen, int dimensionRowY) {
            AbstractWidget best = null;
            for (GuiEventListener child : screen.children()) {
                if (!(child instanceof AbstractWidget wd) || !wd.getClass().getSimpleName().equals("EnderEyeButton")) continue;
                if (wd.y <= dimensionRowY) continue;
                if (best == null || wd.y < best.y || (wd.y == best.y && wd.x < best.x)) best = wd;
            }
            return best;
        }

        /**
         * Panel de guía: título, explicación y la acción que toca. Se coloca donde no tape ningún botón: primero en el
         * sitio preferido (a la izquierda, o abajo a la derecha) y si choca, en las otras esquinas y laterales.
         */
        private static void drawBanner(PoseStack pose, Screen screen, String banner, String action,
                                       boolean bottomRight, int maxWidth) {
            int screenWidth = screen.width, screenHeight = screen.height;
            int room = bottomRight ? screenWidth / 2 - 40 : screenWidth / 2 - 150;
            float scale = room >= 160 ? 1.0F : 0.75F;
            int width = (int) (Math.max(96, Math.min(maxWidth, room)) / scale);
            Component title = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".title");
            Component text = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".text");
            Component todo = action == null ? null : Component.translatable("gui.bte_mobs.welcome.banner.action." + action);
            int height = panelHeight(width, text, todo);
            int pw = (int) ((width + 8) * scale), ph = (int) ((height + 4) * scale);
            int[][] spots = {
                    bottomRight ? new int[]{screenWidth - pw - 8, screenHeight - ph - 10} : new int[]{6, (screenHeight - ph) / 2},
                    {6, (screenHeight - ph) / 2},
                    {6, 6},
                    {6, screenHeight - ph - 6},
                    {screenWidth - pw - 6, (screenHeight - ph) / 2},
                    {screenWidth - pw - 6, 6},
                    {screenWidth - pw - 6, screenHeight - ph - 6}};
            int[] best = spots[0];
            for (int[] spot : spots) {
                if (!coversWidget(screen, spot[0], spot[1], pw, ph)) {
                    best = spot;
                    break;
                }
            }
            drawPanel(pose, best[0] + (int) (4 * scale), Math.max(6, best[1] + (int) (4 * scale)), width, scale, title, text, todo, 0, 0);
        }

        /** Alto (sin escalar) de un panel de ese ancho. */
        private static int panelHeight(int width, Component text, Component todo) {
            Font font = Minecraft.getInstance().font;
            int lines = 1 + font.split(text, width).size() + (todo == null ? 0 : font.split(todo, width).size());
            return lines * 10 + 10;
        }

        /**
         * Dibuja un panel en (x, y) ya escalado. arrow: 0 sin flecha, 1 flecha a la derecha (apunta a lo que hay a su
         * derecha), -1 a la izquierda; arrowY es la altura de la flecha en pantalla.
         */
        private static void drawPanel(PoseStack pose, int x, int y, int width, float scale, Component title, Component text,
                                      Component todo, int arrow, int arrowY) {
            Font font = Minecraft.getInstance().font;
            List<FormattedCharSequence> lines = font.split(text, width);
            List<FormattedCharSequence> todoLines = todo == null ? List.of()
                    : font.split(todo.copy().withStyle(net.minecraft.ChatFormatting.YELLOW), width);
            int height = panelHeight(width, text, todo);
            pose.pushPose();
            pose.translate(x, y, 500);
            pose.scale(scale, scale, 1.0F);
            GuiComponent.fill(pose, -4, -4, width + 4, height, 0xD0100010);
            GuiComponent.fill(pose, -4, -4, width + 4, -3, 0xFFFFD700);
            GuiComponent.fill(pose, -4, height - 1, width + 4, height, 0xFFFFD700);
            int ty = 0;
            font.drawShadow(pose, title, 0, ty, 0xFFD700);
            ty += 12;
            for (FormattedCharSequence line : lines) {
                font.drawShadow(pose, line, 0, ty, 0xFFFFFF);
                ty += 10;
            }
            ty += 2;
            for (FormattedCharSequence line : todoLines) {
                font.drawShadow(pose, line, 0, ty, 0xFFFF55);
                ty += 10;
            }
            pose.popPose();
            if (arrow != 0) {
                // Flecha que sale del panel hacia los botones que explica
                Minecraft mc = Minecraft.getInstance();
                float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
                int bob = (int) (2 * (0.5F + 0.5F * Mth.sin(time * 0.25F)));
                pose.pushPose();
                pose.translate(0, 0, 500);
                float ax = arrow > 0 ? x + (width + 4) * scale + 2 + bob : x - 4 * scale - 9 - bob;
                font.drawShadow(pose, arrow > 0 ? "▶" : "◀", ax, arrowY - 4, 0xFFD700);
                pose.popPose();
            }
        }

        /** Texto del tutorial según el menú de recetas (null = el genérico). */
        private static String eeRecipesBanner(Screen screen) {
            String simple = screen.getClass().getSimpleName();
            switch (simple) {
                case "NatureRecipesScreen", "ExplorerDruidRecipesScreen" -> { return "ee_recipes_nature"; }
                case "BlacksmithRecipesScreen" -> { return "ee_recipes_blacksmith"; }
                case "EnchantmentRecipesScreen" -> {
                    // La misma pantalla sirve para encantamientos y para las pociones del Brujo
                    try {
                        java.lang.reflect.Field f = screen.getClass().getDeclaredField("openPotions");
                        f.setAccessible(true);
                        return f.getBoolean(screen) ? "ee_recipes_potions" : "ee_recipes_enchanting";
                    } catch (Throwable t) {
                        return "ee_recipes_enchanting";
                    }
                }
                default -> { return null; }
            }
        }

        /**
         * Panel pequeño centrado en la parte de abajo del panel morado de la derecha de los menús de recetas
         * (en todos: x = ancho/2 + 8, ancho = ancho - ancho/2 - 24, de y = 22 hasta 28 px antes del borde de abajo).
         */
        private static void drawInRecipePanel(PoseStack pose, Screen screen, String banner, String action) {
            int w = screen.width, h = screen.height;
            int listW = (int) (w * 0.5F);
            int px = listW + 8, pw = w - listW - 24, bottom = h - 28;
            float scale = pw >= 260 ? 0.75F : 0.5F;
            int panelW = Math.min(pw - 16, 300);
            int width = (int) (panelW / scale) - 8;
            Component title = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".title");
            Component text = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".text");
            Component todo = Component.translatable("gui.bte_mobs.welcome.banner.action." + action);
            int ph = (int) ((panelHeight(width, text, todo) + 4) * scale);
            int x = px + (pw - (int) ((width + 8) * scale)) / 2 + (int) (4 * scale);
            int y = bottom - ph - 6 + (int) (4 * scale);
            drawPanel(pose, x, y, width, scale, title, text, todo, 0, 0);
        }

        /**
         * Panel en el hueco libre a la izquierda de un panel centrado de ese ancho, reducido para que quepa.
         * Si no hay hueco suficiente, va abajo a la derecha como el resto.
         */
        private static void drawInLeftGap(PoseStack pose, Screen screen, int centerWidth, String banner, String action) {
            int w = screen.width, h = screen.height;
            int room = (w - centerWidth) / 2 - 12;
            if (room < 70) {
                drawBanner(pose, screen, banner, action, true, 180);
                return;
            }
            float scale = room >= 150 ? 1.0F : (room >= 105 ? 0.75F : 0.5F);
            int width = (int) (Math.min(room, 180) / scale) - 8;
            Component title = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".title");
            Component text = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".text");
            Component todo = Component.translatable("gui.bte_mobs.welcome.banner.action." + action);
            int ph = (int) ((panelHeight(width, text, todo) + 4) * scale);
            int x = 6 + (int) (4 * scale);
            int y = Math.max(6, (h - ph) / 2) + (int) (4 * scale);
            drawPanel(pose, x, y, width, scale, title, text, todo, 0, 0);
        }

        /** ¿Taparía ese rectángulo algún botón visible de la pantalla? */
        private static boolean coversWidget(Screen screen, int x, int y, int w, int h) {
            for (GuiEventListener child : screen.children()) {
                if (!(child instanceof AbstractWidget wd) || !wd.visible) continue;
                if (x < wd.x + wd.getWidth() + 2 && x + w > wd.x - 2 && y < wd.y + wd.getHeight() + 2 && y + h > wd.y - 2) return true;
            }
            return false;
        }

        /**
         * Menú de Ojos de Ender: tres bloques pequeños, cada uno junto a la fila de botones que explica (dimensiones
         * arriba a la izquierda, Ojos de Ender a la derecha, recetas abajo a la izquierda), con una flecha hacia ella.
         * La acción que toca va en su bloque. Ninguno tapa botones.
         */
        private static void drawEeMenuGuide(PoseStack pose, Screen screen, int actionBlock, String action) {
            int w = screen.width, h = screen.height;
            int dimRowY = h / 2 + 13 * -8 - 13;
            int[] dims = null, eyes = null, recipes = null; // minX, minY, maxX, maxY
            int eyeH = 26;
            for (GuiEventListener child : screen.children()) {
                if (!(child instanceof AbstractWidget wd) || !wd.visible) continue;
                String simple = wd.getClass().getSimpleName();
                int[] box = {wd.x, wd.y, wd.x + wd.getWidth(), wd.y + wd.getHeight()};
                if (simple.equals("EnderEyeButton") && wd.y > dimRowY) {
                    eyes = union(eyes, box);
                    eyeH = wd.getHeight();
                }
                else if (EE_RECIPE_BUTTONS.contains(simple) || simple.equals("MatrixStorageButton")) recipes = union(recipes, box);
                else if (wd.y == dimRowY) dims = union(dims, box);
            }
            int leftLimit = Integer.MAX_VALUE;
            for (int[] g : new int[][]{dims, eyes, recipes}) if (g != null) leftLimit = Math.min(leftLimit, g[0]);
            if (leftLimit == Integer.MAX_VALUE) leftLimit = w / 2 - 130;
            String[] keys = {"ee_dims", "ee_eyes", "ee_recipes_row"};
            int[][] groups = {dims, eyes, recipes};
            for (int i = 0; i < 3; i++) {
                int[] g = groups[i];
                // Solo se explica la fila del paso actual (Ojos de Ender, y después las recetas)
                if (g == null || i != actionBlock) continue;
                boolean right = false;
                int left = right ? g[2] + 16 : 6;
                int room = right ? w - 6 - left : leftLimit - 16 - left;
                if (room < 50) continue;
                float scale = room >= 150 ? 1.0F : 0.75F;
                int width = (int) (Math.min(room, 170) / scale) - 8;
                Component title = Component.translatable("gui.bte_mobs.welcome.banner." + keys[i] + ".title");
                Component text = Component.translatable("gui.bte_mobs.welcome.banner." + keys[i] + ".text");
                Component todo = i == actionBlock && action != null
                        ? ("ee_recipes".equals(action)
                            ? Component.translatable("gui.bte_mobs.welcome.banner.action.ee_recipes_progress", EE_RECIPES_SEEN.size(), EE_RECIPE_MENUS.length)
                            : Component.translatable("gui.bte_mobs.welcome.banner.action." + action)) : null;
                int ph = (int) ((panelHeight(width, text, todo) + 4) * scale);
                // Los Ojos de Ender: junto a la primera fila (la del ojo resaltado)
                int centerY = i == 1 ? g[1] + eyeH / 2 : (g[1] + g[3]) / 2;
                int top = Mth.clamp(centerY - ph / 2, 6, Math.max(6, h - ph - 6));
                int x = right ? w - 6 - (int) ((width + 4) * scale) : left + (int) (4 * scale);
                drawPanel(pose, x, top + (int) (4 * scale), width, scale, title, text, todo, right ? -1 : 1, centerY);
            }
        }

        /** Panel pequeño a la derecha de un botón (o a la izquierda si no cabe), con una flecha hacia él. */
        private static void drawNextToWidget(PoseStack pose, Screen screen, AbstractWidget target, String banner, String action) {
            if (target == null) {
                drawBanner(pose, screen, banner, action, false, 200);
                return;
            }
            int w = screen.width, h = screen.height;
            Component title = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".title");
            Component text = Component.translatable("gui.bte_mobs.welcome.banner." + banner + ".text");
            Component todo = Component.translatable("gui.bte_mobs.welcome.banner.action." + action);
            int roomRight = w - 6 - (target.x + target.getWidth() + 16);
            int roomLeft = target.x - 16 - 6;
            boolean right = roomRight >= roomLeft;
            int room = right ? roomRight : roomLeft;
            float scale = room >= 150 ? 1.0F : 0.75F;
            int width = (int) (Math.min(Math.max(room, 60), 170) / scale) - 8;
            int ph = (int) ((panelHeight(width, text, todo) + 4) * scale);
            int centerY = target.y + target.getHeight() / 2;
            int top = Mth.clamp(centerY - ph / 2, 6, Math.max(6, h - ph - 6));
            int x = right ? target.x + target.getWidth() + 16 + (int) (4 * scale)
                    : target.x - 16 - (int) ((width + 4) * scale);
            drawPanel(pose, x, top + (int) (4 * scale), width, scale, title, text, todo, right ? -1 : 1, centerY);
        }

        private static int[] union(int[] a, int[] b) {
            if (a == null) return b;
            return new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])};
        }

        /**
         * ¿Taparía el marco de ese botón el tooltip que se está viendo? El tooltip sale junto al ratón (a su derecha),
         * así que solo se ocultan los marcos que caen en esa zona; el resto sigue visible.
         */
        private static boolean hitsTooltip(Screen screen, double mouseX, double mouseY, int x, int y, int w, int h) {
            if (!mouseOverWidget(screen, mouseX, mouseY)) return false;
            double zx0 = mouseX + 4, zx1 = mouseX + 230, zy0 = mouseY - 26, zy1 = mouseY + 46;
            // Marco (2 px) y flecha (a la izquierda, unos 12 px)
            return x - 14 < zx1 && x + w + 2 > zx0 && y - 2 < zy1 && y + h + 2 > zy0;
        }

        /** Marco dorado que late alrededor del botón del inventario que hay que pulsar. */
        @SubscribeEvent
        public static void onWelcomeScreenRender(ScreenEvent.Render.Post event) {
            WelcomeHandler.Step target = currentTarget();
            if (target == null || revealPending) return;
            boolean teleportStep = target == WelcomeHandler.Step.TELEPORT || target == WelcomeHandler.Step.HOME_RETURN;
            if (!WelcomeHandler.isClientStep(target) && !teleportStep && target != WelcomeHandler.Step.BACKPACK_EQUIP) return;
            if (!(event.getScreen() instanceof InventoryScreen screen)) return;
            Object tomeButton = teleportStep ? forgottenTomeButton() : null;
            if (target == WelcomeHandler.Step.DIM_STORAGE_BUTTON) {
                // El botón del Dimensional Storage lo dibuja su mod encima del inventario (no es un widget)
                int bx = screen.getGuiLeft() + DIM_STORAGE_BTN_X, by = screen.getGuiTop() + DIM_STORAGE_BTN_Y;
                double mx = event.getMouseX(), my = event.getMouseY();
                if (mx >= bx && mx < bx + DIM_STORAGE_BTN_SIZE && my >= by && my < by + DIM_STORAGE_BTN_SIZE) return;
                highlight(event.getPoseStack(), event.getPartialTick(), screen.getGuiLeft() + DIM_STORAGE_BTN_X,
                        screen.getGuiTop() + DIM_STORAGE_BTN_Y, DIM_STORAGE_BTN_SIZE, DIM_STORAGE_BTN_SIZE);
                return;
            }
            for (GuiEventListener child : screen.children()) {
                if (!(child instanceof AbstractWidget widget) || !widget.visible) continue;
                String name = widget.getClass().getName();
                boolean isTarget = switch (target) {
                    case ENDER_EYES, MATRIX -> name.equals(ENDER_EYES_BUTTON);
                    case QUEST_LOG -> name.startsWith(QuestLogButton.class.getName());
                    // Botón de Curios (junto al muñeco) y botón de la mochila del addon
                    case BACKPACK_EQUIP -> widget.getClass().getSimpleName().equals("CuriosButton");
                    case BACKPACK_OPEN -> name.startsWith(BACKPACK_BUTTON);
                    case TELEPORT, HOME_RETURN -> tomeButton != null && widget == tomeButton;
                    default -> false;
                };
                if (!isTarget) continue;
                if (hitsTooltip(screen, event.getMouseX(), event.getMouseY(), widget.x, widget.y, widget.getWidth(), widget.getHeight())) break;
                highlight(event.getPoseStack(), event.getPartialTick(), widget.x, widget.y, widget.getWidth(), widget.getHeight());
                break;
            }
        }

        /** Marco dorado que late alrededor de un botón, con una flecha a su izquierda. */
        private static void highlight(PoseStack pose, float partialTick, int x, int y, int w, int h) {
            Minecraft mc = Minecraft.getInstance();
            float time = (mc.level != null ? mc.level.getGameTime() : 0) + partialTick;
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.25F);
            int alpha = (int) (140 + 115 * pulse);
            int color = (alpha << 24) | 0xFFD700;
            int x0 = x - 2, y0 = y - 2, x1 = x + w + 2, y1 = y + h + 2;
            pose.pushPose();
            pose.translate(0, 0, 400);
            GuiComponent.fill(pose, x0, y0, x1, y0 + 1, color);
            GuiComponent.fill(pose, x0, y1 - 1, x1, y1, color);
            GuiComponent.fill(pose, x0, y0 + 1, x0 + 1, y1 - 1, color);
            GuiComponent.fill(pose, x1 - 1, y0 + 1, x1, y1 - 1, color);
            mc.font.drawShadow(pose, "▶", x0 - 9 - (int) (2 * pulse), y + (h - 8) / 2.0F, 0xFFD700);
            pose.popPose();
        }

        @SubscribeEvent
        public static void onWelcomeLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            active = false;
            talked = 0;
            introTick = -1;
            cardPhase = 0;
            revealPending = false;
            cardPrefix = null;
            introSequence = false;
            announceAfterCards = false;
            doneAfterCards = false;
            obeliskPos = null;
            waystonePos = null;
            terminalPos = null;
            furnacePos = null;
            NEAREST_CACHE.clear();
            EE_RECIPES_SEEN.clear();
            stepHint = null;
            Minecraft.getInstance().getToasts().clear();
            XaeroWaypoints.clear();
            beamTarget = null;
        }
    }

    // ------------------------------------------------------------------ Registro (bus del mod)

    @Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class WelcomeModEvents {
        private WelcomeModEvents() {}

        @SubscribeEvent
        public static void onRegisterWelcomeOverlay(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("welcome_titles", WelcomeClient::render);
        }

        @SubscribeEvent
        public static void onRegisterWelcomeKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(SKIP_KEY);
        }
    }

    // ------------------------------------------------------------------ Títulos

    private static void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        drawStepHint(poseStack, screenWidth);
        if (introTick < 0) return;
        if (cardPhase == 0 || cardIndex >= cards.length) return;
        float pt = phaseTick + (mc.isPaused() ? 0 : partialTick);
        float alpha = switch (cardPhase) {
            case 1 -> pt / FADE_IN;
            case 2 -> 1.0F;
            default -> 1.0F - pt / FADE_OUT;
        };
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        if (alpha <= 0.02F) return;
        int a = Math.max(4, (int) (alpha * 255)) << 24; // con menos de 4 de alfa el texto se dibuja opaco
        int card = cardIndex;

        Font font = mc.font;
        Component title = Component.translatable("gui.bte_mobs.welcome." + cards[card] + ".title");
        Component subtitle = Component.translatable("gui.bte_mobs.welcome." + cards[card] + ".subtitle",
                CARD_ARGS.getOrDefault(cards[card], new Object[0]));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---- Medidas (en coordenadas "virtuales"; luego todo se escala si no cabe por encima de la barra de vida)
        int[] image = CARD_IMAGES.get(cards[card]);
        // Columna central: los lados quedan libres (minimapa, efectos, avisos de la esquina...)
        int colW = Math.min(screenWidth - 40, Math.max(200, (int) (screenWidth * 0.6F)));
        float titleScale = Math.min(image != null ? 2.0F : 3.0F, colW / (float) Math.max(1, font.width(title)));
        titleScale = Math.max(1.0F, titleScale);
        float subScale = image != null ? 1.0F : 1.25F;
        List<FormattedCharSequence> lines = font.split(subtitle, (int) (colW / subScale));
        int titleH = (int) (font.lineHeight * titleScale);
        int subH = (int) (lines.size() * (font.lineHeight + 1) * subScale);
        int dw = 0, dh = 0;
        if (image != null) {
            float s = Math.min(Math.min(colW, 300) / (float) image[0], 140.0F / image[1]);
            dw = Math.round(image[0] * s);
            dh = Math.round(image[1] * s);
        }
        int imageBlock = image != null ? dh + 12 : 0;            // imagen + hueco hasta el título
        int hintsH = 14 + 9 + (active ? 12 : 0);                  // "continuar" (+ "saltar")
        int contentH = imageBlock + titleH + 8 + subH + hintsH;

        // Zona libre: desde arriba hasta encima de la vida, la armadura, la comida y la barra rápida
        int bottomReserve = 64;
        int availTop = 6, availBottom = screenHeight - bottomReserve;
        int avail = Math.max(40, availBottom - availTop);
        float fit = Math.min(1.0F, avail / (float) contentH);
        // Centrado en la zona libre, un poco por encima del centro como antes
        float startY = availTop + Math.max(0.0F, (avail - contentH * fit) * 0.42F);
        float cx = screenWidth / 2.0F;
        poseStack.pushPose();
        poseStack.translate(cx, startY, 0);
        poseStack.scale(fit, fit, 1.0F);
        poseStack.translate(-cx, 0, 0);
        // A partir de aquí y = 0 es la parte de arriba del bloque

        int y0 = 0;
        if (image != null) {
            // Imagen centrada encima del título, con marco dorado; entra con un pequeño zoom
            int top = y0;
            int left = (screenWidth - dw) / 2;
            float zoom = cardPhase == 1 ? 0.92F + 0.08F * alpha : 1.0F;
            poseStack.pushPose();
            poseStack.translate(cx, top + dh / 2.0F, 0);
            poseStack.scale(zoom, zoom, 1.0F);
            poseStack.translate(-cx, -(top + dh / 2.0F), 0);
            int frameA = (int) (alpha * 0xB0) << 24;
            GuiComponent.fill(poseStack, left - 4, top - 4, left + dw + 4, top + dh + 4, frameA);
            int border = Math.max(4, (int) (alpha * 255)) << 24 | 0xC9A227;
            GuiComponent.fill(poseStack, left - 4, top - 4, left + dw + 4, top - 3, border);
            GuiComponent.fill(poseStack, left - 4, top + dh + 3, left + dw + 4, top + dh + 4, border);
            GuiComponent.fill(poseStack, left - 4, top - 3, left - 3, top + dh + 3, border);
            GuiComponent.fill(poseStack, left + dw + 3, top - 3, left + dw + 4, top + dh + 3, border);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID, "textures/gui/guide/" + cards[card] + ".png"));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            GuiComponent.blit(poseStack, left, top, dw, dh, 0, 0, image[0], image[1], image[0], image[1]);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }

        // Título grande
        int centerY = imageBlock + titleH;
        poseStack.pushPose();
        poseStack.translate(cx, centerY, 0);
        poseStack.scale(titleScale, titleScale, 1.0F);
        font.drawShadow(poseStack, title, -font.width(title) / 2.0F, -font.lineHeight, 0xFFD700 | a);
        poseStack.popPose();

        // Subtítulo partido en líneas para que nunca se salga de la pantalla
        poseStack.pushPose();
        poseStack.translate(cx, centerY + 8, 0);
        poseStack.scale(subScale, subScale, 1.0F);
        int y = 0;
        for (FormattedCharSequence line : lines) {
            font.drawShadow(poseStack, line, -font.width(line) / 2.0F, y, 0xFFFFFF | a);
            y += font.lineHeight + 1;
        }
        poseStack.popPose();

        // Debajo: tecla para continuar (late suavemente cuando ya se puede pulsar) y cómo saltar el tutorial
        int hintY = centerY + 8 + subH + 14;
        boolean ready = cardPhase == 2 && phaseTick >= MIN_STAY;
        float blink = ready ? 0.6F + 0.4F * (0.5F + 0.5F * Mth.sin((mc.player.tickCount + partialTick) * 0.2F)) : 0.35F;
        int ha = Math.max(4, (int) (alpha * blink * 255)) << 24;
        Component cont = Component.translatable("gui.bte_mobs.welcome.continue", mc.options.keyAttack.getTranslatedKeyMessage());
        font.drawShadow(poseStack, cont, (screenWidth - font.width(cont)) / 2.0F, hintY, 0xFFD700 | ha);
        if (active) {
            int sa = Math.max(4, (int) (alpha * 0.6F * 255)) << 24;
            Component skip = Component.translatable("gui.bte_mobs.welcome.skip_hint", SKIP_KEY.getTranslatedKeyMessage());
            font.drawShadow(poseStack, skip, (screenWidth - font.width(skip)) / 2.0F, hintY + 12, 0xAAAAAA | sa);
        }
        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}
