package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.quest.WelcomeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: el jugador ha mantenido la tecla de saltar el tutorial; se da por terminado. */
public class WelcomeSkipPacket {

    public static void encode(WelcomeSkipPacket msg, FriendlyByteBuf buf) {
    }

    public static WelcomeSkipPacket decode(FriendlyByteBuf buf) {
        return new WelcomeSkipPacket();
    }

    public static void handle(WelcomeSkipPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player != null) WelcomeHandler.skip(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
