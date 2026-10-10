package fr.shoqapik.btemobs.quest;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.WelcomePacket;
import net.minecraft.nbt.CompoundTag;
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
 * 2. Después, un objetivo aparte (no es una quest): hablar con los 4 NPCs del Lobby, en cualquier orden. Cada NPC
 *    lleva un "!" dorado hasta que se habla con él, y el objetivo sale en el panel de seguimiento.
 * 3. Al hablar con los 4 sale un aviso que lleva al inventario (Ojos de Ender y libro de quests).
 *
 * El estado se guarda por jugador en sus datos persistentes (se mantiene al morir).
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WelcomeHandler {

    private WelcomeHandler() {}

    private static final String TAG = "bte_mobs_welcome";
    private static final String SHOWN = "shown";
    private static final String TALKED = "talked";
    private static final String DONE = "done";

    /** Los 4 NPCs del objetivo. */
    public static final BteNpcType[] NPCS = {BteNpcType.BLACKSMITH, BteNpcType.EXPLORER, BteNpcType.WARLOCK, BteNpcType.DRUID};
    public static final int ALL = (1 << NPCS.length) - 1;

    public static int bit(BteNpcType type) {
        for (int i = 0; i < NPCS.length; i++) if (NPCS[i] == type) return 1 << i;
        return 0;
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
        boolean firstTime = !tag.getBoolean(SHOWN);
        tag.putBoolean(SHOWN, true);
        BteMobsMod.sendToClient(new WelcomePacket(firstTime, tag.getInt(TALKED), false), player);
    }

    /** Hablar con un NPC del Lobby cuenta para el objetivo (se mira antes de que el NPC procese el clic). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof BteAbstractEntity npc)) return;
        int bit = bit(npc.getNpcType());
        if (bit == 0) return;
        CompoundTag tag = data(player);
        if (tag.getBoolean(DONE)) return;
        int talked = tag.getInt(TALKED);
        if ((talked & bit) != 0) return;
        talked |= bit;
        tag.putInt(TALKED, talked);
        boolean done = talked == ALL;
        if (done) tag.putBoolean(DONE, true);
        BteMobsMod.sendToClient(new WelcomePacket(false, talked, done), player);
    }

    /** Debug: vuelve a empezar la bienvenida del jugador (títulos incluidos). */
    public static void reset(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putBoolean(SHOWN, true);
        tag.putInt(TALKED, 0);
        tag.putBoolean(DONE, false);
        BteMobsMod.sendToClient(new WelcomePacket(true, 0, false), player);
    }
}
