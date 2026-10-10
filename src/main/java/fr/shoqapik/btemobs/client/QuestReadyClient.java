package fr.shoqapik.btemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.gui.QuestReadyToast;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.QuestReadyPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Cliente: aviso en la esquina cuando una quest queda lista para reclamar, y un "!" dorado sobre la cabeza de los
 * NPCs que tienen alguna quest lista.
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestReadyClient {

    private QuestReadyClient() {}

    /** NPCs con alguna quest lista para reclamar (los envía el servidor). */
    private static final Set<BteNpcType> READY_NPCS = EnumSet.noneOf(BteNpcType.class);

    private static final double MARKER_RANGE = 64.0D;
    private static final int MARKER_COLOR = 0xFFD700;

    /** true si el NPC tiene alguna quest lista para reclamar. */
    public static boolean hasReady(BteNpcType type) {
        return READY_NPCS.contains(type);
    }

    public static void handle(QuestReadyPacket msg) {
        READY_NPCS.clear();
        READY_NPCS.addAll(msg.npcsWithReady);

        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < msg.newQuestIds.size(); i++) {
            String id = msg.newQuestIds.get(i);
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            String key = "title.quest." + path;
            Component title = I18n.exists(key) ? Component.translatable(key) : Component.literal(path.replace('_', ' '));
            mc.getToasts().addToast(new QuestReadyToast(title, msg.newQuestNpcs.get(i)));
        }
        if (!msg.newQuestIds.isEmpty()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.4F, 0.6F));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        READY_NPCS.clear();
    }

    /** "!" dorado flotando sobre los NPCs con quests listas para reclamar (y, en la bienvenida, los que aún no conoce). */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (READY_NPCS.isEmpty() && !WelcomeClient.anyPending()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partialTick = event.getPartialTick();
        AABB area = new AABB(cam, cam).inflate(MARKER_RANGE);
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        String mark = "!";
        float bob = (float) Math.sin((level.getGameTime() + partialTick) / 8.0D) * 0.08F;

        for (BteAbstractEntity npc : level.getEntitiesOfClass(BteAbstractEntity.class, area)) {
            // Quest lista para reclamar, o NPC con el que aún no ha hablado (bienvenida)
            boolean marked = READY_NPCS.contains(npc.getNpcType()) || WelcomeClient.pending(npc.getNpcType());
            if (!marked || npc.isInvisible()) continue;
            double x = npc.xOld + (npc.getX() - npc.xOld) * partialTick - cam.x;
            double y = npc.yOld + (npc.getY() - npc.yOld) * partialTick - cam.y + npc.getBbHeight() + 0.55D + extraHeight(npc.getNpcType()) + bob;
            double z = npc.zOld + (npc.getZ() - npc.zOld) * partialTick - cam.z;

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(camera.rotation());
            pose.scale(-0.05F, -0.05F, 0.05F);
            Matrix4f matrix = pose.last().pose();
            float half = -font.width(mark) / 2.0F;
            // Sombra oscura + texto dorado, visible a través de bloques cercanos para que se vea desde lejos
            font.drawInBatch(mark, half + 1, 1, 0x3F2A00, false, matrix, buffers, true, 0, LightTexture.FULL_BRIGHT);
            font.drawInBatch(mark, half, 0, MARKER_COLOR, false, matrix, buffers, true, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }

    /** Altura extra del "!" por NPC: el modelo de algunos sobresale de su caja de colisión (pelo, capucha, corona). */
    private static double extraHeight(BteNpcType type) {
        return switch (type) {
            case BLACKSMITH -> 0.5D;
            case WARLOCK -> 0.5D;
            case DRUID -> 0.9D;
            default -> 0.0D;
        };
    }

    /** Nombre del NPC para textos (Anna, Antonio, Noah, Oriana). */
    public static String npcName(BteNpcType type) {
        return I18n.get("entity.bte_mobs." + type.name().toLowerCase(Locale.ROOT));
    }
}
