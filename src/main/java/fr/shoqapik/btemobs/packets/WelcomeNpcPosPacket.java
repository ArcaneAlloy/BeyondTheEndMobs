package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente: dónde está el NPC con el que toca hablar en el tutorial.
 *
 * El cliente solo conoce las entidades que tiene cerca (su distancia de seguimiento), así que sin esto el haz de luz y
 * el waypoint del NPC no salían hasta acercarse. Con esta posición se dibujan desde lejos; cuando el NPC ya está
 * cargado en el cliente se usa su posición real.
 */
public class WelcomeNpcPosPacket {
    public final BteNpcType npc;
    public final ResourceLocation dimension;
    public final double x, y, z;

    public WelcomeNpcPosPacket(BteNpcType npc, ResourceLocation dimension, double x, double y, double z) {
        this.npc = npc;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(WelcomeNpcPosPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.npc);
        buf.writeResourceLocation(msg.dimension);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static WelcomeNpcPosPacket decode(FriendlyByteBuf buf) {
        return new WelcomeNpcPosPacket(buf.readEnum(BteNpcType.class), buf.readResourceLocation(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(WelcomeNpcPosPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> fr.shoqapik.btemobs.client.WelcomeClient.setNpcPos(msg.npc, msg.dimension, msg.x, msg.y, msg.z)));
        ctx.get().setPacketHandled(true);
    }
}
