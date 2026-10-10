package fr.shoqapik.btemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import fr.shoqapik.btemobs.entity.DruidEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Oriana (Druida): en vez de objetos al azar girando a su alrededor, luciérnagas del Twilight Forest.
 *
 * - Tres luciérnagas orbitan despacio sobre su cabeza. Se dibujan directamente (una luz por luciérnaga, con la
 *   textura de las luciérnagas del Twilight Forest y un brillo que late), sin estela, para que no quede cargado.
 * - De vez en cuando aparece una luciérnaga errante ("twilightforest:wandering_firefly") revoloteando cerca de ella.
 *
 * Solo cliente.
 */
public final class DruidWisps {

    private DruidWisps() {}

    private static final int FIREFLIES = 3;
    private static final double RADIUS = 1.15D;
    private static final double BASE_HEIGHT = 2.35D;
    /** Radianes por tick (una vuelta cada ~10 s). */
    private static final double SPEED = Math.PI * 2.0D / 200.0D;
    /** Medio lado de la luz, en bloques. */
    private static final float HALF_SIZE = 0.12F;

    private static final ResourceLocation FIREFLY_TEXTURE =
            new ResourceLocation("twilightforest", "textures/particle/firefly.png");
    private static final ResourceLocation WANDERING_ID = new ResourceLocation("twilightforest", "wandering_firefly");

    private static ParticleOptions wandering;
    private static Boolean twilightLoaded;

    private static boolean twilight() {
        if (twilightLoaded == null) twilightLoaded = ModList.get().isLoaded("twilightforest");
        return twilightLoaded;
    }

    // ------------------------------------------------------------------ Luciérnagas en órbita (render)

    /** Dibuja las luciérnagas. poseStack en el origen de la entidad, sin girar con ella. */
    public static void render(DruidEntity druid, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                              Quaternion cameraRotation) {
        if (!twilight() || druid.isInvisible()) return;
        float time = druid.tickCount + partialTick;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(FIREFLY_TEXTURE));
        for (int i = 0; i < FIREFLIES; i++) {
            double angle = time * SPEED + i * (Math.PI * 2.0D / FIREFLIES);
            // Cada luciérnaga sube y baja con su propio desfase, y el radio "respira" un poco
            double radius = RADIUS + Mth.sin(time * 0.04F + i * 2.1F) * 0.15D;
            double x = Math.cos(angle) * radius;
            double y = BASE_HEIGHT + Mth.sin(time * 0.06F + i * 2.1F) * 0.3D;
            double z = Math.sin(angle) * radius;
            // Brillo que late, como las luciérnagas de verdad (cada una a su ritmo)
            float glow = 0.55F + 0.45F * (0.5F + 0.5F * Mth.sin(time * 0.15F + i * 1.7F));

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(cameraRotation);
            quad(vc, poseStack, HALF_SIZE, glow);
            poseStack.popPose();
        }
    }

    private static void quad(VertexConsumer vc, PoseStack poseStack, float s, float glow) {
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();
        // Las dos caras, para que no desaparezca según hacia dónde mire la cámara
        vertex(vc, m, n, -s, -s, 0, 1, glow);
        vertex(vc, m, n, s, -s, 1, 1, glow);
        vertex(vc, m, n, s, s, 1, 0, glow);
        vertex(vc, m, n, -s, s, 0, 0, glow);
        vertex(vc, m, n, -s, s, 0, 0, glow);
        vertex(vc, m, n, s, s, 1, 0, glow);
        vertex(vc, m, n, s, -s, 1, 1, glow);
        vertex(vc, m, n, -s, -s, 0, 1, glow);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, float glow) {
        vc.vertex(m, x, y, 0.0F).color(glow, glow, glow, 1.0F).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    // ------------------------------------------------------------------ Luciérnagas errantes (partículas)

    /** Llamado desde DruidEntity.tick() cuando level.isClientSide. */
    public static void tick(DruidEntity druid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || druid.isInvisible()) return;
        ParticleStatus status = mc.options.particles().get();
        if (status != ParticleStatus.ALL) return;
        if (mc.player != null && mc.player.distanceToSqr(druid) > 32 * 32) return;
        if (wandering == null) {
            ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(WANDERING_ID);
            wandering = type instanceof SimpleParticleType simple ? simple : ParticleTypes.GLOW;
        }
        RandomSource random = druid.getRandom();
        if (random.nextInt(60) != 0) return;
        double a = random.nextDouble() * Math.PI * 2.0D;
        double r = 0.8D + random.nextDouble() * 1.0D;
        mc.particleEngine.createParticle(wandering,
                druid.getX() + Math.cos(a) * r, druid.getY() + 1.2D + random.nextDouble() * 1.8D,
                druid.getZ() + Math.sin(a) * r,
                (random.nextDouble() - 0.5D) * 0.02D, (random.nextDouble() - 0.5D) * 0.02D,
                (random.nextDouble() - 0.5D) * 0.02D);
    }
}
