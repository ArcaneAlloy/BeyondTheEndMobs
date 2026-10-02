package fr.shoqapik.btemobs.compat;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/**
 * Mutant More: al morir, el Mutant Wither Skeleton se rompe en partes que se recogen con clic derecho.
 * La calavera daba mutantmore:mutant_wither_skeleton_skull (el casco de la armadura mutante, que en el modpack
 * se fabrica en el Herrero). Aquí la calavera da una Calavera de Esqueleto Wither vanilla.
 * Las demás partes (pelvis, costilla, extremidad, hombrera) siguen igual.
 *
 * Se hace por id de entidad y reflexión para no depender de las clases de Mutant More al compilar.
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MutantWitherSkullHandler {

    private static final ResourceLocation BODY_PART =
            new ResourceLocation("mutantmore", "mutant_wither_skeleton_body_part");
    private static final ResourceLocation MUTANT_SKULL =
            new ResourceLocation("mutantmore", "mutant_wither_skeleton_skull");

    private static Method getItemByPart;
    private static boolean lookupFailed;

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!BODY_PART.equals(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()))) return;
        if (!target.isAlive() || !isSkull(target)) return;

        if (!target.level.isClientSide) {
            ItemEntity drop = target.spawnAtLocation(Items.WITHER_SKELETON_SKULL);
            if (drop != null) drop.setNoPickUpDelay();
            target.discard();
        }
        event.getEntity().swing(event.getHand());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** true si la parte es la calavera (getItemByPart() devuelve la calavera mutante). */
    private static boolean isSkull(Entity part) {
        if (lookupFailed) return false;
        try {
            if (getItemByPart == null) getItemByPart = part.getClass().getMethod("getItemByPart");
            Object item = getItemByPart.invoke(part);
            return item instanceof Item i && MUTANT_SKULL.equals(ForgeRegistries.ITEMS.getKey(i));
        } catch (ReflectiveOperationException e) {
            lookupFailed = true;
            BteMobsMod.LOGGER.warn("Mutant More: no se pudo leer la parte del Mutant Wither Skeleton ({})", e.toString());
            return false;
        }
    }
}
