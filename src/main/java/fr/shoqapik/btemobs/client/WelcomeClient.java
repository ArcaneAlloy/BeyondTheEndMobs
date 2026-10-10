package fr.shoqapik.btemobs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
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
    private static final ResourceLocation ENDER_EYES_ICON =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/quest_log/ender_eyes.png");

    /** Títulos de la bienvenida: gui.bte_mobs.welcome.<n>.title / .subtitle */
    private static final int CARDS = 3;
    /** Ticks antes del primer título (tras cargar el mundo), y duración de cada uno. */
    private static final int START_DELAY = 50;
    private static final int FADE_IN = 12, STAY = 70, FADE_OUT = 14, GAP = 8;
    private static final int CARD_TICKS = FADE_IN + STAY + FADE_OUT + GAP;

    /** Objetivo activo (hasta hablar con los 4). */
    private static boolean active = false;
    /** NPCs con los que ya ha hablado (bits de WelcomeHandler.NPCS). */
    private static int talked = 0;
    /** Ticks de la secuencia de títulos (-1 = no hay títulos pendientes). */
    private static int introTick = -1;

    public static void handle(WelcomePacket msg) {
        talked = msg.talked;
        if (msg.done) {
            active = false;
            introTick = -1;
            Minecraft mc = Minecraft.getInstance();
            Component key = mc.options.keyInventory.getTranslatedKeyMessage();
            mc.getToasts().addToast(new QuestReadyToast(
                    Component.translatable("toast.bte_mobs.welcome_done", key),
                    Component.translatable("toast.bte_mobs.welcome_done.desc"),
                    ENDER_EYES_ICON));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.6F));
            return;
        }
        active = true;
        if (msg.intro) introTick = 0;
    }

    /** true mientras se muestran los títulos: el "!" y el objetivo salen al acabar. */
    private static boolean introRunning() {
        return introTick >= 0;
    }

    /** "!" sobre este NPC: aún no ha hablado con él. */
    public static boolean pending(BteNpcType type) {
        int bit = WelcomeHandler.bit(type);
        return active && !introRunning() && bit != 0 && (talked & bit) == 0;
    }

    /** ¿Hay algún NPC marcado por la bienvenida? */
    public static boolean anyPending() {
        return active && !introRunning() && talked != WelcomeHandler.ALL;
    }

    /** ¿Se muestra el objetivo en el panel de seguimiento? */
    public static boolean objectiveVisible() {
        return anyPending();
    }

    public static int talkedCount() {
        return Integer.bitCount(talked & WelcomeHandler.ALL);
    }

    public static boolean talkedTo(BteNpcType type) {
        return (talked & WelcomeHandler.bit(type)) != 0;
    }

    // ------------------------------------------------------------------ Eventos (bus de Forge)

    @Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class WelcomeForgeEvents {
        private WelcomeForgeEvents() {}

        @SubscribeEvent
        public static void onWelcomeClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || introTick < 0) return;
            Minecraft mc = Minecraft.getInstance();
            // Se espera a que el mundo esté cargado y no haya pantallas abiertas (carga, menú de pausa...)
            if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()) return;
            introTick++;
            int t = introTick - START_DELAY;
            if (t >= 0 && t % CARD_TICKS == 0 && t / CARD_TICKS < CARDS) {
                int card = t / CARD_TICKS;
                float pitch = card == CARDS - 1 ? 1.2F : 0.9F;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 1.0F));
            }
            if (t >= CARDS * CARD_TICKS) {
                introTick = -1;
                // Aparecen los "!" sobre los NPCs
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.6F));
            }
        }

        @SubscribeEvent
        public static void onWelcomeLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            active = false;
            talked = 0;
            introTick = -1;
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
    }

    // ------------------------------------------------------------------ Títulos

    private static void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (introTick < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        float t = introTick - START_DELAY + (mc.isPaused() ? 0 : partialTick);
        if (t < 0) return;
        int card = (int) (t / CARD_TICKS);
        if (card >= CARDS) return;
        float local = t - card * CARD_TICKS;
        float alpha;
        if (local < FADE_IN) alpha = local / FADE_IN;
        else if (local < FADE_IN + STAY) alpha = 1.0F;
        else if (local < FADE_IN + STAY + FADE_OUT) alpha = 1.0F - (local - FADE_IN - STAY) / FADE_OUT;
        else return;
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        if (alpha <= 0.02F) return;
        int a = Math.max(4, (int) (alpha * 255)) << 24; // con menos de 4 de alfa el texto se dibuja opaco

        Font font = mc.font;
        Component title = Component.translatable("gui.bte_mobs.welcome." + (card + 1) + ".title");
        Component subtitle = Component.translatable("gui.bte_mobs.welcome." + (card + 1) + ".subtitle");

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Título grande (se reduce si no cabe en pantallas estrechas)
        float titleScale = Math.min(4.0F, (screenWidth - 24) / (float) Math.max(1, font.width(title)));
        titleScale = Math.max(1.5F, titleScale);
        int centerY = screenHeight / 2 - 30;
        poseStack.pushPose();
        poseStack.translate(screenWidth / 2.0F, centerY, 0);
        poseStack.scale(titleScale, titleScale, 1.0F);
        font.drawShadow(poseStack, title, -font.width(title) / 2.0F, -font.lineHeight, 0xFFD700 | a);
        poseStack.popPose();

        // Subtítulo partido en líneas para que nunca se salga de la pantalla
        float subScale = 1.5F;
        int maxWidth = (int) ((screenWidth - 40) / subScale);
        List<FormattedCharSequence> lines = font.split(subtitle, maxWidth);
        poseStack.pushPose();
        poseStack.translate(screenWidth / 2.0F, centerY + 8, 0);
        poseStack.scale(subScale, subScale, 1.0F);
        int y = 0;
        for (FormattedCharSequence line : lines) {
            font.drawShadow(poseStack, line, -font.width(line) / 2.0F, y, 0xFFFFFF | a);
            y += font.lineHeight + 1;
        }
        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}
