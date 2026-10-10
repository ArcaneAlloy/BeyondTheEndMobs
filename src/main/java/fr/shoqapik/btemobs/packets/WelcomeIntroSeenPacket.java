package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.quest.WelcomeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: el jugador ha pasado los títulos de bienvenida; no vuelven a salir al entrar al mundo. */
public class WelcomeIntroSeenPacket {

    public static void encode(WelcomeIntroSeenPacket msg, FriendlyByteBuf buf) {
    }

    public static WelcomeIntroSeenPacket decode(FriendlyByteBuf buf) {
        return new WelcomeIntroSeenPacket();
    }

    public static void handle(WelcomeIntroSeenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player != null) WelcomeHandler.introSeen(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
