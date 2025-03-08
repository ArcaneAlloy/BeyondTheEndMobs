package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPacket {

    public final int id;
    public final BlockPos pos;

    public SyncPacket(int id,BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(this::handlePlaceRecipePacket);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public void handlePlaceRecipePacket(){
        Entity entity = Minecraft.getInstance().level.getEntity(id);
        if(entity instanceof ExplorerEntity explorer &&
                Minecraft.getInstance().level.getBlockEntity(pos) instanceof ExplorerTableBlockEntity tableBlock){
            explorer.setTablePos(pos);
            tableBlock.setData(0,id);
        }
    }

    public static SyncPacket decode(FriendlyByteBuf packetBuffer) {
        return new SyncPacket(packetBuffer.readInt(),packetBuffer.readBlockPos());
    }

    public static void encode(SyncPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(msg.id);
        packetBuffer.writeBlockPos(msg.pos);
    }

}
