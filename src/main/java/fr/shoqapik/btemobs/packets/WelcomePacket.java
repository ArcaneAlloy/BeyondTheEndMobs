package fr.shoqapik.btemobs.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente: estado de la bienvenida (ver WelcomeHandler).
 * intro: mostrar los títulos; talked: NPCs con los que ya ha hablado (bits); done: objetivo recién completado.
 */
public class WelcomePacket {
    public final boolean intro;
    public final int talked;
    public final boolean done;

    public WelcomePacket(boolean intro, int talked, boolean done) {
        this.intro = intro;
        this.talked = talked;
        this.done = done;
    }

    public static void encode(WelcomePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.intro);
        buf.writeVarInt(msg.talked);
        buf.writeBoolean(msg.done);
    }

    public static WelcomePacket decode(FriendlyByteBuf buf) {
        return new WelcomePacket(buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(WelcomePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> fr.shoqapik.btemobs.client.WelcomeClient.handle(msg)));
        ctx.get().setPacketHandled(true);
    }
}
