package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Servidor -> cliente: quests que acaban de quedar listas para reclamar (aviso en pantalla) y NPCs que tienen
 * alguna quest lista (marcador "!" sobre su cabeza).
 */
public class QuestReadyPacket {
    public final List<String> newQuestIds;
    public final List<BteNpcType> newQuestNpcs;
    public final Set<BteNpcType> npcsWithReady;

    public QuestReadyPacket(List<String> newQuestIds, List<BteNpcType> newQuestNpcs, Set<BteNpcType> npcsWithReady) {
        this.newQuestIds = newQuestIds;
        this.newQuestNpcs = newQuestNpcs;
        this.npcsWithReady = npcsWithReady;
    }

    public static void encode(QuestReadyPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.newQuestIds.size());
        for (int i = 0; i < msg.newQuestIds.size(); i++) {
            buf.writeUtf(msg.newQuestIds.get(i));
            buf.writeEnum(msg.newQuestNpcs.get(i));
        }
        buf.writeInt(msg.npcsWithReady.size());
        for (BteNpcType type : msg.npcsWithReady) buf.writeEnum(type);
    }

    public static QuestReadyPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<String> ids = new ArrayList<>(n);
        List<BteNpcType> npcs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUtf());
            npcs.add(buf.readEnum(BteNpcType.class));
        }
        int m = buf.readInt();
        Set<BteNpcType> ready = EnumSet.noneOf(BteNpcType.class);
        for (int i = 0; i < m; i++) ready.add(buf.readEnum(BteNpcType.class));
        return new QuestReadyPacket(ids, npcs, ready);
    }

    public static void handle(QuestReadyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> fr.shoqapik.btemobs.client.QuestReadyClient.handle(msg)));
        ctx.get().setPacketHandled(true);
    }
}
