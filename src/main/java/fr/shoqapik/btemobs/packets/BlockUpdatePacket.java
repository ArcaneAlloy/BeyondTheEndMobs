package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlockUpdatePacket {

    public final BlockPos pos;

    public BlockUpdatePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void handle(BlockUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()->BlockUpdatePacket.handlePlaceRecipePacket(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handlePlaceRecipePacket(BlockUpdatePacket msg){
        if(Minecraft.getInstance().level.getBlockEntity(msg.pos) instanceof ExplorerTableBlockEntity tableBlock){
            tableBlock.setItem(ItemStack.EMPTY);
        }
    }



    public static BlockUpdatePacket decode(FriendlyByteBuf packetBuffer) {
        return new BlockUpdatePacket(packetBuffer.readBlockPos());
    }

    public static void encode(BlockUpdatePacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeBlockPos(msg.pos);
    }


}
