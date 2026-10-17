package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.WelcomePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bienvenida al entrar por primera vez en un mundo.
 *
 * 1. El cliente muestra unos títulos en el centro de la pantalla: hay que reunir los 24 Ojos de Ender para abrir el
 *    portal Beyond The End, y los habitantes del Lobby ayudarán.
 * 2. Después, un objetivo aparte (no es una quest), EN ORDEN: hablar con Anna, Antonio y Noah, abrir el Obelisco
 *    de Experiencia que hay junto a Noah y hablar con Oriana. Lo que toca lleva un haz de luz tipo faro (y un "!"
 *    dorado si es un NPC), y el objetivo sale en el panel de seguimiento. Hacerlo fuera de orden no cuenta.
 * 3. Al hablar con los 4 sale un aviso que lleva al inventario (Ojos de Ender y libro de quests).
 *
 * Los títulos se repiten en cada entrada hasta que hable con el primer NPC del recorrido.
 * El estado se guarda por jugador en sus datos persistentes (se mantiene al morir).
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WelcomeHandler {

    private WelcomeHandler() {}

    private static final String TAG = "bte_mobs_welcome";
    private static final String SHOWN = "shown";
    private static final String TALKED = "talked";
    private static final String DONE = "done";
    /** Ya ha pasado los títulos de bienvenida (no vuelven a salir al entrar al mundo). */
    private static final String INTRO = "intro_seen";

    /** Bloque del paso del Obelisco de Experiencia. */
    public static final ResourceLocation OBELISK_ID = new ResourceLocation("experienceobelisk", "experience_obelisk");

    /**
     * Pasos del recorrido. El bit de cada paso es su posición en el enum (no cambiar el orden de las constantes, se
     * guarda en los datos del jugador); el orden en que hay que hacerlos es ORDER.
     */
    public enum Step {
        BLACKSMITH(BteNpcType.BLACKSMITH),
        EXPLORER(BteNpcType.EXPLORER),
        WARLOCK(BteNpcType.WARLOCK),
        DRUID(BteNpcType.DRUID),
        /** Abrir el Obelisco de Experiencia que hay junto a Noah. */
        OBELISK(null),
        /** Abrir el menú de Ojos de Ender con su botón del inventario (lo detecta el cliente). */
        ENDER_EYES(null),
        /** Abrir el libro de quests con su botón del inventario (lo detecta el cliente). */
        QUEST_LOG(null),
        /** Usar el botón "Teleport to The Forgotten Realm" del inventario (Forgotten Tome Menu). */
        TELEPORT(null),
        /** Activar la waystone del Lobby (la que queda delante al llegar con el teletransporte). */
        WAYSTONE(null),
        /** Ir a su Home Zone (la zona de construcción, en x≈5000 del Lobby). */
        HOME_ZONE(null),
        /** Volver al Lobby desde la Home Zone con el botón de teletransporte. */
        HOME_RETURN(null),
        /** Abrir el Crafting Terminal de la Storage Room (Tom's Simple Storage). */
        STORAGE(null),
        /** Abrir el Jumbo Furnace que hay a la derecha del Crafting Terminal. */
        FURNACE(null),
        /** Abrir el Dimensional Storage (Dimensional Storage Actuator de Occultism) con clic derecho. */
        DIM_STORAGE(null),
        /** Abrir el Dimensional Storage con su botón del inventario (lo detecta el cliente). */
        DIM_STORAGE_BUTTON(null),
        /** Entrar en la sala del Summoning Altar (lo detecta el cliente: se acerca al altar). */
        ALTAR(null),
        /** Ir al ascensor del Lobby (planta de arriba). */
        ELEVATOR(null),
        /** Bajar con el ascensor a la segunda planta: la Portal Room. */
        PORTAL_ROOM(null),
        /** Bajar a la tercera planta: el portal Beyond The End. */
        BTE_PORTAL(null),
        /** Dentro del menú de Ojos de Ender: abrir la dimensión del Twilight Forest (lo detecta el cliente). */
        EE_DIMENSION(null),
        /** Dentro del menú de Ojos de Ender: abrir uno de los Ojos (lo detecta el cliente). */
        EE_EYE(null),
        /** Dentro del menú de Ojos de Ender: abrir uno de los menús de recetas (lo detecta el cliente). */
        EE_RECIPES(null),
        /** Abrir el menú para cambiar el nombre de la waystone (mano vacía + Shift + clic derecho; lo detecta el cliente). */
        WAYSTONE_RENAME(null),
        /** Abrir el Matrix Storage desde el menú de Ojos de Ender (tiers del Dimensional Storage; lo detecta el cliente). */
        MATRIX(null),
        /** Hablar con el Noah de la Portal Room (NPC5). */
        PORTAL_NOAH(BteNpcType.NPC5),
        /** Volver a subir con el ascensor a la Portal Room (piso 2) después de ver el portal Beyond The End. */
        BACK_TO_PORTAL_ROOM(null),
        /** Mochila de Backpacked: equiparla en la casilla "Back" de Curios (se salta sola si ya la lleva). */
        BACKPACK_EQUIP(null),
        /** Abrir la mochila (botón del inventario del addon btebackpackbutton, o su tecla). */
        BACKPACK_OPEN(null),
        /** Abrir "Customize" dentro de la mochila (skins de la mochila). */
        BACKPACK_SKINS(null),
        /** Llegar a la Storage Room (antes de abrir el Crafting Terminal); lo detecta el cliente por cercanía. */
        STORAGE_ROOM(null);

        /** NPC del paso, o null si no es hablar con un NPC. */
        public final BteNpcType npc;

        Step(BteNpcType npc) {
            this.npc = npc;
        }

        public int bit() {
            return 1 << ordinal();
        }
    }

    /**
     * Orden del recorrido: Anna, Antonio, Noah, Obelisco de Experiencia (junto a Noah), Oriana, y después los botones
     * del inventario: menú de Ojos de Ender (y dentro de él: el Twilight Forest, un Ojo y un menú de recetas), libro de quests y "Teleport to The Forgotten Realm", y por último
     * activar la waystone del Lobby, visitar la Home Zone y volver, y en la Storage Room abrir el Crafting Terminal, el Jumbo Furnace y el Dimensional Storage (con clic derecho y
     * con su botón del inventario), entrar en la sala del Summoning Altar bajar con el ascensor a la Portal Room y hablar
     * allí con Noah. BTE_PORTAL y BACK_TO_PORTAL_ROOM ya no están en el orden (se podía hablar con Noah antes de bajar al portal).
     */
    public static final Step[] ORDER = {Step.BLACKSMITH, Step.EXPLORER, Step.WARLOCK, Step.OBELISK, Step.DRUID,
            Step.ENDER_EYES, Step.EE_EYE, Step.EE_RECIPES, Step.QUEST_LOG, Step.TELEPORT, Step.WAYSTONE, Step.WAYSTONE_RENAME, Step.HOME_ZONE, Step.HOME_RETURN,
            Step.STORAGE_ROOM, Step.STORAGE, Step.FURNACE, Step.DIM_STORAGE, Step.DIM_STORAGE_BUTTON, Step.MATRIX,
            Step.BACKPACK_EQUIP, Step.BACKPACK_OPEN, Step.BACKPACK_SKINS, Step.ALTAR, Step.ELEVATOR,
            Step.PORTAL_ROOM};

    /**
     * Ascensor del Lobby (bloques de ascensor, p. ej. "Orange Elevator"): tres plantas, la de arriba (y≈112), la
     * Portal Room (y≈80) y la del portal Beyond The End (y≈53).
     */
    public static boolean isElevator(ResourceLocation id) {
        return id != null && (id.getNamespace().startsWith("elevator") || id.getPath().contains("elevator"));
    }

    /** Sala del ascensor de la planta de arriba del Lobby (donde se busca el ascensor para el haz de luz). */
    public static final net.minecraft.core.BlockPos ELEVATOR_TOP = new net.minecraft.core.BlockPos(0, 112, -38);

    /** Planta del ascensor en la que está el jugador (0 arriba, 1 Portal Room, 2 portal), o -1 si no está sobre él. */
    public static int elevatorFloor(ServerPlayer player) {
        net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(player.getX(), player.getY() - 0.2D, player.getZ());
        if (!isElevator(ForgeRegistries.BLOCKS.getKey(player.level.getBlockState(below).getBlock()))) return -1;
        double y = player.getY();
        return y >= 100 ? 0 : y >= 67 ? 1 : 2;
    }

    /** Summoning Altar (mod summoningrituals: el normal o el indestructible del Lobby). */
    public static boolean isAltar(ResourceLocation id) {
        return id != null && id.getNamespace().equals("summoningrituals") && id.getPath().contains("altar");
    }

    /** Dimensional Storage Actuator (Occultism) de la Storage Room. */
    public static final ResourceLocation DIM_STORAGE_ID = new ResourceLocation("occultism", "storage_controller");

    /** Bloques del Jumbo Furnace (multibloque de 3x3x3, todos del mod jumbofurnace). */
    public static boolean isJumboFurnace(ResourceLocation id) {
        return id != null && id.getNamespace().equals("jumbofurnace") && id.getPath().startsWith("jumbo_furnace");
    }

    /** Teletransportador a la Home Zone (Simple Teleporters), junto a la waystone del Lobby. */
    public static final ResourceLocation HOME_TELEPORTER_ID = new ResourceLocation("teleporters", "teleporter");

    /** Crafting Terminal de la Storage Room. */
    public static final ResourceLocation CRAFTING_TERMINAL_ID = new ResourceLocation("toms_storage", "ts.crafting_terminal");
    /** La Home Zone empieza en x≈5000 del Lobby (chunks 300-320); todo lo que esté más allá de x=4800 cuenta. */
    public static final double HOME_ZONE_MIN_X = 4800.0D;

    /**
     * Hablar con Anna, Antonio, Noah u Oriana: el paso cuenta cuando el jugador ve la guía del NPC (botón de su
     * diálogo) o confirma que se la salta. Lo avisa el cliente.
     */
    public static boolean isGuideStep(Step step) {
        return step == Step.BLACKSMITH || step == Step.EXPLORER || step == Step.WARLOCK || step == Step.DRUID;
    }

    /** Pasos que detecta el cliente (pantallas que solo existen en el cliente) y avisa con WelcomeStepPacket. */
    public static boolean isClientStep(Step step) {
        return isGuideStep(step) || step == Step.ENDER_EYES || step == Step.EE_DIMENSION || step == Step.EE_EYE || step == Step.EE_RECIPES
                || step == Step.QUEST_LOG || step == Step.DIM_STORAGE_BUTTON || step == Step.ALTAR
                || step == Step.WAYSTONE_RENAME || step == Step.MATRIX
                || step == Step.BACKPACK_OPEN || step == Step.BACKPACK_SKINS || step == Step.STORAGE_ROOM;
    }

    /** El cliente avisa de que ha abierto la pantalla de un paso (solo cuenta si es el que toca). */
    public static void completeFromClient(ServerPlayer player, Step step) {
        if (step == null || !isClientStep(step)) return;
        complete(player, current -> current == step);
    }
    public static final int ALL;
    static {
        int all = 0;
        for (Step step : ORDER) all |= step.bit();
        ALL = all;
    }

    /** Paso que toca ahora (el primero de ORDER sin hacer), o null si ya están todos. */
    public static Step current(int done) {
        for (Step step : ORDER) if ((done & step.bit()) == 0) return step;
        return null;
    }

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = persisted.getCompound(TAG);
        persisted.put(TAG, tag);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return tag;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag tag = data(player);
        if (tag.getBoolean(DONE)) return;
        tag.putBoolean(SHOWN, true);
        // Los títulos de bienvenida salen una sola vez: hasta que el jugador los pasa (el cliente avisa al acabar).
        // Si sale del mundo a mitad, vuelven a salir. Después, al entrar solo se restauran el haz, el "!" y el objetivo.
        int talked = tag.getInt(TALKED);
        // Si se han quitado pasos del orden (dimensiones, Noah de la Portal Room) puede que ya lo tenga todo hecho
        if (talked != 0 && current(talked) == null) {
            tag.putBoolean(DONE, true);
            return;
        }
        boolean intro = talked == 0 && !tag.getBoolean(INTRO);
        BteMobsMod.sendToClient(new WelcomePacket(intro, talked, false), player);
        NPC_POS_SENT.remove(player.getUUID());
        Step step = current(talked);
        if (step != null && step.npc != null) sendNpcPos(player, step.npc);
    }

    /** Hablar con un NPC del Lobby cuenta para el objetivo (se mira antes de que el NPC procese el clic). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof BteAbstractEntity npc)) return;
        // Los NPCs con guía no cuentan al hablar: cuentan al ver (o saltar) su guía desde el diálogo
        complete(player, step -> step.npc != null && step.npc == npc.getNpcType() && !isGuideStep(step));
    }

    /** Abrir (clic derecho) el Obelisco de Experiencia o el Crafting Terminal cuenta para su paso. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(player.level.getBlockState(event.getPos()).getBlock());
        if (OBELISK_ID.equals(id)) complete(player, step -> step == Step.OBELISK);
        else if (CRAFTING_TERMINAL_ID.equals(id)) complete(player, step -> step == Step.STORAGE);
        else if (isJumboFurnace(id)) complete(player, step -> step == Step.FURNACE);
        else if (DIM_STORAGE_ID.equals(id)) complete(player, step -> step == Step.DIM_STORAGE);
    }

    /** Completa el paso que toca si cumple la condición (solo cuenta en orden). */
    private static void complete(ServerPlayer player, java.util.function.Predicate<Step> matches) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(DONE)) return;
        int done = tag.getInt(TALKED);
        Step step = current(done);
        if (step == null || !matches.test(step)) return;
        done |= step.bit();
        tag.putInt(TALKED, done);
        tag.putBoolean(INTRO, true);
        boolean finished = current(done) == null;
        if (finished) tag.putBoolean(DONE, true);
        BteMobsMod.sendToClient(new WelcomePacket(false, done, finished), player);
        // El haz del NPC siguiente sale ya, aunque esté lejos
        Step next = current(done);
        if (!finished && next != null && next.npc != null) sendNpcPos(player, next.npc);
    }

    // ------------------------------------------------------------------ Pasos del teletransporte y de la waystone

    /** Punto de llegada del botón "Teleport to The Forgotten Realm" (Forgotten Tome Menu). */
    private static final ResourceLocation LOBBY_ID = new ResourceLocation("ender_journey", "the_forgotten_realm");
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> LOBBY =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY, LOBBY_ID);
    private static final double TP_X = 0.0D, TP_Y = 116.0D, TP_Z = 0.0D;
    /** Distancia al punto de llegada para darlo por bueno, y salto mínimo entre dos ticks para que sea un teletransporte. */
    private static final double ARRIVAL_RADIUS_SQR = 3.0D * 3.0D;
    private static final double MIN_JUMP_SQR = 6.0D * 6.0D;
    private static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> LAST_POS = new java.util.HashMap<>();

    /**
     * El botón teletransporta al jugador al punto de llegada del Lobby (0 116 0). No hay evento para eso, así que
     * mientras toque este paso se mira cada tick si el jugador ha "saltado" de golpe hasta ese punto.
     */
    @SubscribeEvent
    public static void onWelcomePlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        net.minecraft.world.phys.Vec3 pos = player.position();
        net.minecraft.world.phys.Vec3 last = LAST_POS.put(player.getUUID(), pos);
        // Pasos por posición (waystone del Lobby, ascensor, Home Zone): se miran cada medio segundo
        if (player.tickCount % 10 == 0) {
            CompoundTag tag = data(player);
            Step step = tag.getBoolean(DONE) ? null : current(tag.getInt(TALKED));
            if (step != null && step.npc != null && player.tickCount % 20 == 0) sendNpcPos(player, step.npc);
            boolean inLobby = player.level.dimension().location().equals(LOBBY_ID);
            if (step == Step.WAYSTONE && QuestTriggers.hasActiveWaystoneIn(player, LOBBY)) {
                complete(player, s -> s == Step.WAYSTONE);
            } else if (step == Step.ELEVATOR && inLobby && elevatorFloor(player) == 0) {
                complete(player, s -> s == Step.ELEVATOR);
            } else if (step == Step.PORTAL_ROOM && inLobby && elevatorFloor(player) == 1) {
                complete(player, s -> s == Step.PORTAL_ROOM);
            } else if (step == Step.BTE_PORTAL && inLobby && elevatorFloor(player) == 2) {
                complete(player, s -> s == Step.BTE_PORTAL);
            } else if (step == Step.BACK_TO_PORTAL_ROOM && inLobby && elevatorFloor(player) == 1) {
                complete(player, s -> s == Step.BACK_TO_PORTAL_ROOM);
            } else if (step == Step.BACKPACK_EQUIP || step == Step.BACKPACK_OPEN || step == Step.BACKPACK_SKINS) {
                tickBackpack(player, tag, step);
            } else if (step == Step.HOME_ZONE && inLobby && pos.x > HOME_ZONE_MIN_X) {
                // Ya está en su Home Zone
                complete(player, s -> s == Step.HOME_ZONE);
            }
        }
        if (last == null || last.distanceToSqr(pos) < MIN_JUMP_SQR) return;
        if (!player.level.dimension().location().equals(LOBBY_ID)) return;
        if (pos.distanceToSqr(TP_X, TP_Y, TP_Z) > ARRIVAL_RADIUS_SQR) return;
        // Teletransporte al Lobby: primero para aprenderlo, y después para volver de la Home Zone
        complete(player, step -> step == Step.TELEPORT || step == Step.HOME_RETURN);
    }

    /** Si llega al Lobby desde otra dimensión con el botón, también cuenta. */
    @SubscribeEvent
    public static void onWelcomeChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getTo().location().equals(LOBBY_ID)) return;
        if (player.position().distanceToSqr(TP_X, TP_Y, TP_Z) > ARRIVAL_RADIUS_SQR) return;
        complete(player, step -> step == Step.TELEPORT || step == Step.HOME_RETURN);
    }

    // ------------------------------------------------------------------ Mochila (Backpacked)

    private static final String BACKPACK_GIVEN = "backpack_given";
    private static final ResourceLocation BACKPACK_ID = new ResourceLocation("backpacked", "backpack");
    private static java.lang.reflect.Method getBackpackStack;
    private static boolean backpackLookedUp = false;

    /**
     * Mochila equipada (Backpacked.getBackpackStack, por reflexión: no hay dependencia del mod).
     * null si Backpacked no está o su API ha cambiado.
     */
    private static net.minecraft.world.item.ItemStack equippedBackpack(ServerPlayer player) {
        if (!backpackLookedUp) {
            backpackLookedUp = true;
            try {
                if (net.minecraftforge.fml.ModList.get().isLoaded("backpacked")) {
                    getBackpackStack = Class.forName("com.mrcrayfish.backpacked.Backpacked")
                            .getMethod("getBackpackStack", Player.class);
                }
            } catch (Throwable ignored) {
                getBackpackStack = null;
            }
        }
        if (getBackpackStack == null) return null;
        try {
            Object stack = getBackpackStack.invoke(null, player);
            return stack instanceof net.minecraft.world.item.ItemStack s ? s : net.minecraft.world.item.ItemStack.EMPTY;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hasBackpackItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (BACKPACK_ID.equals(ForgeRegistries.ITEMS.getKey(player.getInventory().getItem(i).getItem()))) return true;
        }
        return false;
    }

    /**
     * Pasos de la mochila. Sin Backpacked se dan por hechos. Equiparla cuenta en cuanto la lleva puesta (si ya la
     * llevaba, el paso se salta solo); si no tiene ninguna mochila, se le da una (una sola vez).
     */
    private static void tickBackpack(ServerPlayer player, CompoundTag tag, Step step) {
        net.minecraft.world.item.ItemStack worn = equippedBackpack(player);
        if (worn == null) {
            complete(player, s -> s == step);
            return;
        }
        if (step != Step.BACKPACK_EQUIP) return;
        if (!worn.isEmpty()) {
            complete(player, s -> s == Step.BACKPACK_EQUIP);
        } else if (!tag.getBoolean(BACKPACK_GIVEN) && !hasBackpackItem(player)) {
            tag.putBoolean(BACKPACK_GIVEN, true);
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(BACKPACK_ID);
            if (item != null) player.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(item));
        }
    }

    // ------------------------------------------------------------------ Posición del NPC que toca (haz desde lejos)

    /** Última posición conocida de cada NPC por dimensión (los NPCs del Lobby no se mueven del sitio). */
    private static final java.util.Map<String, net.minecraft.world.phys.Vec3> NPC_POS = new java.util.HashMap<>();
    /** Lo último que se ha mandado a cada jugador, para no repetirlo cada segundo. */
    private static final java.util.Map<java.util.UUID, String> NPC_POS_SENT = new java.util.HashMap<>();

    /**
     * El cliente solo tiene las entidades que están a unos pocos chunks, así que el haz y el waypoint del NPC no salían
     * hasta acercarse. El servidor sí lo tiene cargado (está dentro de la distancia de visión): se busca aquí y se le
     * manda su posición al cliente. Si ahora no está cargado se usa la última posición conocida.
     */
    private static void sendNpcPos(ServerPlayer player, BteNpcType type) {
        net.minecraft.server.level.ServerLevel level = player.getLevel();
        String dimension = level.dimension().location().toString();
        String key = dimension + "|" + type.name();
        net.minecraft.world.phys.Vec3 pos = null;
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof BteAbstractEntity npc) || npc.getNpcType() != type || !npc.isAlive()) continue;
            double dist = npc.distanceToSqr(player);
            if (dist < best) {
                best = dist;
                pos = npc.position();
            }
        }
        if (pos != null) NPC_POS.put(key, pos);
        else pos = NPC_POS.get(key);
        if (pos == null) return;
        String sent = key + "|" + (int) Math.floor(pos.x) + "|" + (int) Math.floor(pos.y) + "|" + (int) Math.floor(pos.z);
        if (sent.equals(NPC_POS_SENT.get(player.getUUID()))) return;
        NPC_POS_SENT.put(player.getUUID(), sent);
        BteMobsMod.sendToClient(new fr.shoqapik.btemobs.packets.WelcomeNpcPosPacket(type, level.dimension().location(),
                pos.x, pos.y, pos.z), player);
    }

    /** Al reaparecer tras morir no cuenta como teletransporte. */
    @SubscribeEvent
    public static void onWelcomeRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST_POS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onWelcomeLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POS.remove(event.getEntity().getUUID());
        NPC_POS_SENT.remove(event.getEntity().getUUID());
    }

    /** El cliente ha terminado de mostrar los títulos de bienvenida. */
    public static void introSeen(ServerPlayer player) {
        data(player).putBoolean(INTRO, true);
    }

    /** El jugador ha saltado el tutorial (manteniendo la tecla): se da por terminado y no vuelve a salir. */
    public static void skip(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putBoolean(SHOWN, true);
        tag.putInt(TALKED, ALL);
        tag.putBoolean(DONE, true);
    }

    /** Debug: vuelve a empezar la bienvenida del jugador (títulos incluidos). */
    public static void reset(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putBoolean(SHOWN, true);
        tag.putInt(TALKED, 0);
        tag.putBoolean(DONE, false);
        tag.putBoolean(INTRO, false);
        BteMobsMod.sendToClient(new WelcomePacket(true, 0, false), player);
    }
}
