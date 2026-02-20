package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CheckUnlockRecipePacket {

    private boolean hasOpenedRecipeBook = false;

    public static void handle(CheckUnlockRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                BteMobsMod.handleUnlockRecipePacket(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }


    public static CheckUnlockRecipePacket decode(FriendlyByteBuf packetBuffer) {
        //String recipe = packetBuffer.readUtf();
        return new CheckUnlockRecipePacket();
    }

    public static void encode(CheckUnlockRecipePacket msg, FriendlyByteBuf packetBuffer) {
        //packetBuffer.writeUtf(msg.recipe);
    }



}
