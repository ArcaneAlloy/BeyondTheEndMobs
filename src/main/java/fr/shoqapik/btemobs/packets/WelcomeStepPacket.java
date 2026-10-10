package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.quest.WelcomeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente -> servidor: el jugador ha abierto la pantalla de un paso de la bienvenida que solo se ve en el cliente
 * (menú de Ojos de Ender, libro de quests). El servidor solo lo cuenta si es el paso que toca.
 */
public class WelcomeStepPacket {
    public final WelcomeHandler.Step step;

    public WelcomeStepPacket(WelcomeHandler.Step step) {
        this.step = step;
    }

    public static void encode(WelcomeStepPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.step);
    }

    public static WelcomeStepPacket decode(FriendlyByteBuf buf) {
        return new WelcomeStepPacket(buf.readEnum(WelcomeHandler.Step.class));
    }

    public static void handle(WelcomeStepPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player != null) WelcomeHandler.completeFromClient(player, msg.step);
        });
        ctx.get().setPacketHandled(true);
    }
}
