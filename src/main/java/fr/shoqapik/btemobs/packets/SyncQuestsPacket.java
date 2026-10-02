package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> cliente: definiciones de las quests (los JSON de data/bte_mobs/quest solo se cargan en el servidor).
 * Sin esto, en un servidor dedicado o como invitado LAN el cliente no conoce ninguna quest.
 */
public class SyncQuestsPacket {
    public final List<Quest> quests;

    public SyncQuestsPacket(List<Quest> quests) {
        this.quests = quests;
    }

    public static void encode(SyncQuestsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.quests.size());
        for (Quest quest : msg.quests) Quest.encode(quest, buf);
    }

    public static SyncQuestsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Quest> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(Quest.decode(buf));
        return new SyncQuestsPacket(list);
    }

    public static void handle(SyncQuestsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // En un solo jugador / host LAN el cliente comparte la lista del servidor integrado: no se toca
            if (ServerLifecycleHooks.getCurrentServer() != null) return;
            QuestManager.setClientQuests(msg.quests);
        });
        ctx.get().setPacketHandled(true);
    }
}
