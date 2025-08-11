package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.entity.DruidEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DirectionPacket {

    public int entityId;
    public int direction3d;

    public DirectionPacket(int entityId, int direction3d) {
        this.entityId = entityId;
        this.direction3d = direction3d;
    }

    public static void handle(DirectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() ->{
                Minecraft mc = Minecraft.getInstance();
                assert mc.level!=null;
                Entity entity = mc.level.getEntity(msg.entityId);
                Direction direction = Direction.from3DDataValue(msg.direction3d);
                if(entity instanceof DruidEntity entity1){
                    entity1.setAttachFace(direction);
                    entity1.setYRot(direction.toYRot());
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }


    public static DirectionPacket decode(FriendlyByteBuf packetBuffer) {
        int entityId = packetBuffer.readInt();
        int direction3d = packetBuffer.readInt();
        return new DirectionPacket(entityId, direction3d);
    }

    public static void encode(DirectionPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(msg.entityId);
        packetBuffer.writeInt(msg.direction3d);
    }


}
