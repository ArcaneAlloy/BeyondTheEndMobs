package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.ServerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import twilightforest.init.TFBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Recompensa de la quest del Lich: abre el portal al Twilight Forest en la fuente del Lobby (ender_journey:the_forgotten_realm).
 *
 * El portal ocupa el rombo de ladrillos de la fuente: todos los bloques con |x| + |z| <= RADIUS alrededor de CENTER.
 * El portal de Twilight Forest se deshace (vuelve a agua) si al actualizarse un vecino no tiene debajo un bloque
 * sólido o a los lados algo que no sea tierra/césped u otro bloque de portal. Por eso:
 *  - el suelo de debajo se pone de piedra si no es sólido;
 *  - los bloques del borde exterior que no sean tierra/césped se cambian por césped.
 * Una sola vez por mundo (ServerData).
 */
public final class TwilightPortalUnlock {

    private TwilightPortalUnlock() {}

    /** Centro de la fuente del Lobby. */
    public static final BlockPos CENTER = new BlockPos(0, 78, 0);
    /** Radio del rombo (3 = los 25 ladrillos de la fuente). */
    public static final int RADIUS = 3;

    /** Dimensión del Lobby (es también la originDimension del Twilight Forest, a la que se vuelve). */
    public static final ResourceKey<Level> LOBBY = ResourceKey.create(Registry.DIMENSION_REGISTRY,
            new ResourceLocation("ender_journey", "the_forgotten_realm"));

    /** Recompensa de la quest: abre el portal una sola vez por mundo. */
    public static void open(MinecraftServer server) {
        if (ServerData.get().unlockTwilightForestPortal()) return;
        build(server);
    }

    /** Construye el portal aunque ya se hubiera abierto (comando /btequests twilightportal). false si no existe el Lobby. */
    public static boolean build(MinecraftServer server) {
        ServerLevel level = server.getLevel(LOBBY);
        if (level == null) {
            BteMobsMod.LOGGER.error("No existe la dimensión {}: no se puede abrir el portal al Twilight Forest", LOBBY.location());
            return false;
        }

        List<BlockPos> portal = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= RADIUS) portal.add(CENTER.offset(dx, 0, dz));
            }
        }

        BlockState portalState = TFBlocks.TWILIGHT_PORTAL.get().defaultBlockState();
        int fixedFloor = 0, fixedEdge = 0;

        // 1) Suelo sólido debajo
        for (BlockPos pos : portal) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(below, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                fixedFloor++;
            }
        }
        // 2) Borde exterior de tierra/césped
        for (BlockPos pos : portal) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(dir);
                if (portal.contains(side)) continue;
                if (!level.getBlockState(side).is(BlockTags.DIRT)) {
                    level.setBlock(side, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    fixedEdge++;
                }
            }
        }
        // 3) Portal (flag 2: sin actualizar vecinos, se envía a los clientes) y aire encima
        for (BlockPos pos : portal) {
            level.setBlock(pos, portalState, 2);
            if (!level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        ServerData.get().unlockPortal();
        BteMobsMod.LOGGER.info("Portal al Twilight Forest abierto en {} ({} bloques, {} suelos y {} bordes corregidos)",
                CENTER, portal.size(), fixedFloor, fixedEdge);

        // Efectos: rayo decorativo, sonido y aviso a todos
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(CENTER.getX() + 0.5D, CENTER.getY() + 1, CENTER.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, CENTER, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.bte_mobs.twilight_portal_opened").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return true;
    }
}
