package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import fr.shoqapik.btemobs.SacredPlaceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Detección de las tareas especiales de las quests (lado servidor).
 *
 *  CRAFT_UNIQUE   -> onItemCrafted        (crafteo en Anna/Noah y mesa de crafteo vanilla)
 *  BREW_POTION    -> onWarlockPotionBrewed (salida de la interfaz de pociones del Warlock)
 *  FEED_ENTITY    -> onEntityFed           (clic derecho a una entidad con un item)
 *  PERSONAL_HORSE -> tickPlayer            (comprueba Callable Horses cada 2 segundos)
 *  OBTAIN         -> tickPlayer            (revisa el inventario cada segundo)
 *  WARLOCK_ENCHANT-> onWarlockEnchant      (al sacar el item encantado de la interfaz del Warlock)
 *  ACTIVATE_WAYSTONE -> tickPlayer         (waystones activadas fuera del Lobby, cada 2 segundos)
 */
public final class QuestTriggers {

    private QuestTriggers() {}

    /** true si el stack cumple la especificación: "mod:item" o "#mod:tag". */
    public static boolean matchesItem(String spec, ItemStack stack) {
        if (spec == null || spec.isEmpty() || stack.isEmpty()) return false;
        if (spec.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(spec.substring(1));
            return tag != null && stack.is(ItemTags.create(tag));
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.toString().equals(spec);
    }

    // ---------------------------------------------------------------- CRAFT_UNIQUE

    public static void onItemCrafted(Player player, ItemStack result) {
        if (!(player instanceof ServerPlayer) || result.isEmpty()) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null) return;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(result.getItem());
        if (itemId == null) return;
        String key = itemId.toString();
        cap.updateTasks(TaskData.Type.CRAFT_UNIQUE, task -> {
            if (!matchesItem(task.id, result)) return false;
            if (!task.seen.add(key)) return false; // ya contado
            task.count = task.seen.size();
            return true;
        });
    }

    // ---------------------------------------------------------------- BLACKSMITH_CRAFT

    /** Llamado al craftear o mejorar un item en la interfaz de Anna (BlacksmithCraftMenu). */
    public static void onBlacksmithCraft(Player player, ItemStack result) {
        if (!(player instanceof ServerPlayer) || result.isEmpty()) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null) return;
        cap.updateTasks(TaskData.Type.BLACKSMITH_CRAFT, task -> {
            if (!matchesItem(task.id, result)) return false;
            task.count++;
            return true;
        });
    }

    // ---------------------------------------------------------------- BREW_POTION

    public static void onWarlockPotionBrewed(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null) return;
        cap.updateTasks(TaskData.Type.BREW_POTION, task -> {
            task.count++;
            return true;
        });
    }

    // ---------------------------------------------------------------- WARLOCK_ENCHANT

    public static void onWarlockEnchant(Player player, ItemStack result) {
        if (!(player instanceof ServerPlayer)) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null) return;
        cap.updateTasks(TaskData.Type.WARLOCK_ENCHANT, task -> {
            task.count++;
            return true;
        });
    }

    // ---------------------------------------------------------------- OBTAIN

    /** Marca como completadas las tareas OBTAIN cuyo item está ahora en el inventario del jugador. */
    private static void checkObtain(ServerPlayer player, RecipeCapability<?> cap) {
        cap.updateTasks(TaskData.Type.OBTAIN, task -> {
            for (ItemStack stack : player.getInventory().items) {
                if (matchesItem(task.id, stack)) { task.count = task.maxCount; return true; }
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (matchesItem(task.id, stack)) { task.count = task.maxCount; return true; }
            }
            return false;
        });
    }

    // ---------------------------------------------------------------- FEED_ENTITY

    /** Llamado desde PlayerInteractEvent.EntityInteract (antes de que la entidad procese el clic). */
    public static void onEntityFed(Player player, Entity target, ItemStack held) {
        if (!(player instanceof ServerPlayer) || held.isEmpty()) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null || !cap.hasPendingTask(TaskData.Type.FEED_ENTITY)) return;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityId == null) return;
        // Entidades como el Bunfungus solo aceptan comida si no tienen ya algo en la mano
        if (target instanceof LivingEntity living && !living.getMainHandItem().isEmpty()) return;

        cap.updateTasks(TaskData.Type.FEED_ENTITY, task -> {
            String[] parts = task.id.split("\\|", 2);
            if (parts.length != 2) return false;
            if (!parts[0].equals(entityId.toString())) return false;
            if (!matchesItem(parts[1], held)) return false;
            task.count++;
            return true;
        });
    }

    // ---------------------------------------------------------------- PERSONAL_HORSE

    private static boolean horseLookupFailed = false;
    private static Method getOwnerCap;
    private static Method getStorageUUID;

    /** Llamado en el tick del jugador (servidor). */
    public static void tickPlayer(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        RecipeCapability<?> cap = RecipeCapability.get(player);
        if (cap == null) return;
        cap.checkAdvancementConditions(player);
        cap.checkBossKillAdvancements(player);
        if (cap.hasPendingTask(TaskData.Type.OBTAIN)) checkObtain(player, cap);
        QuestReadyNotifier.tick(player, cap);
        if (player.tickCount % 40 != 0) return;
        if (cap.hasPendingTask(TaskData.Type.ACTIVATE_WAYSTONE)) checkWaystones(player, cap);
        if (cap.hasPendingTask(TaskData.Type.PERSONAL_HORSE) && hasPersonalHorse(player)) {
            cap.updateTasks(TaskData.Type.PERSONAL_HORSE, task -> {
                task.count = task.maxCount;
                return true;
            });
        }
    }

    // ---------------------------------------------------------------- ACTIVATE_WAYSTONE

    private static boolean waystoneLookupFailed = false;
    private static Method getWaystones;
    private static Method getWaystoneDimension;
    private static Method getWaystonePos;

    /**
     * Revisa las waystones que tiene activadas el jugador (mod Waystones, por reflexión para no depender de él al compilar).
     * Cuentan las que no están en el Lobby (the_forgotten_realm). Si la tarea indica una estructura ("icono|mod:estructura"),
     * la waystone además tiene que estar dentro de esa estructura; eso solo se comprueba con el chunk cargado, que es lo
     * normal justo después de activarla.
     */
    private static void checkWaystones(ServerPlayer player, RecipeCapability<?> cap) {
        List<net.minecraft.core.GlobalPos> active = activeWaystonesOutsideLobby(player);
        if (active.isEmpty()) return;
        cap.updateTasks(TaskData.Type.ACTIVATE_WAYSTONE, task -> {
            int bar = task.id == null ? -1 : task.id.indexOf('|');
            String structureId = bar >= 0 ? task.id.substring(bar + 1) : "";
            for (net.minecraft.core.GlobalPos waystone : active) {
                if (structureId.isEmpty() || isInsideStructure(player, waystone, structureId)) {
                    task.count = task.maxCount;
                    return true;
                }
            }
            return false;
        });
    }

    /** true si el jugador tiene activada alguna waystone en esa dimensión (bienvenida: la waystone del Lobby). */
    public static boolean hasActiveWaystoneIn(ServerPlayer player, ResourceKey<Level> dimension) {
        for (net.minecraft.core.GlobalPos pos : activeWaystones(player, true)) {
            if (pos.dimension().equals(dimension)) return true;
        }
        return false;
    }

    private static List<net.minecraft.core.GlobalPos> activeWaystonesOutsideLobby(ServerPlayer player) {
        return activeWaystones(player, false);
    }

    @SuppressWarnings("unchecked")
    private static List<net.minecraft.core.GlobalPos> activeWaystones(ServerPlayer player, boolean includeLobby) {
        List<net.minecraft.core.GlobalPos> result = new ArrayList<>();
        if (waystoneLookupFailed) return result;
        try {
            if (getWaystones == null) {
                Class<?> manager = Class.forName("net.blay09.mods.waystones.core.PlayerWaystoneManager");
                Class<?> waystone = Class.forName("net.blay09.mods.waystones.api.IWaystone");
                getWaystones = manager.getMethod("getWaystones", Player.class);
                getWaystoneDimension = waystone.getMethod("getDimension");
                getWaystonePos = waystone.getMethod("getPos");
            }
            Object list = getWaystones.invoke(null, player);
            if (!(list instanceof List<?> waystones)) return result;
            for (Object waystone : waystones) {
                Object dim = getWaystoneDimension.invoke(waystone);
                Object pos = getWaystonePos.invoke(waystone);
                if (!(dim instanceof ResourceKey<?> key) || !(pos instanceof BlockPos blockPos)) continue;
                if (!includeLobby && key.equals(SacredPlaceHandler.FORGOTTEN_REALM)) continue;
                result.add(net.minecraft.core.GlobalPos.of((ResourceKey<Level>) key, blockPos));
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            waystoneLookupFailed = true;
            BteMobsMod.LOGGER.warn("Waystones no está disponible: las tareas ACTIVATE_WAYSTONE no se pueden completar", e);
        } catch (Exception ignored) {
        }
        return result;
    }

    /** true si la posición está dentro (o justo encima, margen de 4 bloques) de la estructura indicada. */
    private static boolean isInsideStructure(ServerPlayer player, net.minecraft.core.GlobalPos where, String structureId) {
        ServerLevel level = player.server.getLevel(where.dimension());
        ResourceLocation id = ResourceLocation.tryParse(structureId);
        if (level == null || id == null || !level.hasChunkAt(where.pos())) return false;
        Structure structure = level.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).get(id);
        if (structure == null) return false;
        if (level.structureManager().getStructureAt(where.pos(), structure).isValid()) return true;
        for (StructureStart start : level.structureManager().startsForStructure(SectionPos.of(where.pos()), structure)) {
            if (start.isValid() && start.getBoundingBox().inflatedBy(4).isInside(where.pos())) return true;
        }
        return false;
    }

    /**
     * Callable Horses guarda en la capability del jugador el "storageUUID" del caballo marcado como personal.
     * Si está vacío, el jugador no tiene Personal Horse. Se accede por reflexión para no depender del mod al compilar.
     */
    private static boolean hasPersonalHorse(Player player) {
        if (horseLookupFailed) return false;
        try {
            if (getOwnerCap == null) {
                Class<?> helper = Class.forName("tschipp.callablehorses.common.helper.HorseHelper");
                Class<?> owner = Class.forName("tschipp.callablehorses.common.capabilities.horseowner.IHorseOwner");
                getOwnerCap = helper.getMethod("getOwnerCap", Player.class);
                getStorageUUID = owner.getMethod("getStorageUUID");
            }
            Object ownerCap = getOwnerCap.invoke(null, player);
            if (ownerCap == null) return false;
            Object uuid = getStorageUUID.invoke(ownerCap);
            return uuid instanceof String s && !s.isEmpty();
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            horseLookupFailed = true;
            BteMobsMod.LOGGER.warn("Callable Horses no está disponible: las tareas PERSONAL_HORSE no se pueden completar", e);
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
