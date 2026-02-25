package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.BteMobsModClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ActionPacket {

    public int entityId;
    public String actionType;

    public ActionPacket(int entityId, String actionType) {
        this.entityId = entityId;
        this.actionType = actionType;
    }

    public static void handle(ActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() ->
                    BteMobsModClient.handleActionPacket(msg, ctx)
            );
        } else {
            ctx.get().enqueueWork(() ->
                    BteMobsMod.handleActionPacket(msg, ctx)
            );
        }
        ctx.get().setPacketHandled(true);
    }


    public static ActionPacket decode(FriendlyByteBuf packetBuffer) {
        int entityId = packetBuffer.readInt();
        String actionType = packetBuffer.readUtf();
        return new ActionPacket(entityId, actionType);
    }

    public static void encode(ActionPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(msg.entityId);
        packetBuffer.writeUtf(msg.actionType);
    }


}
