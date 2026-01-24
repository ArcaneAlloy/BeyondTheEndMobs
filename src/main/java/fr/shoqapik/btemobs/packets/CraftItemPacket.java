package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CraftItemPacket {

    public final ResourceLocation recipe;

    public CraftItemPacket(Recipe<?> recipe) {
        if(recipe == null) {
            this.recipe = null;
        } else {
            this.recipe = recipe.getId();
        }
    }

    public CraftItemPacket(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    public static void handle(CraftItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                BteMobsMod.handleCraftItemPacket(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }


    public static CraftItemPacket decode(FriendlyByteBuf packetBuffer) {
        if(packetBuffer.readableBytes() != 0) {
            return new CraftItemPacket(packetBuffer.readResourceLocation());
        } else {
            throw new IllegalArgumentException("CraftItemPacket error 111");
        }
    }

    public static void encode(CraftItemPacket msg, FriendlyByteBuf packetBuffer) {
        if(msg.recipe != null) {
            packetBuffer.writeResourceLocation(msg.recipe);
        }
    }
}
