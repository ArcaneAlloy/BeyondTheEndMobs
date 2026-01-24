package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LastClickedRecipeUpdatePacket {

    public final ResourceLocation recipe;

    public LastClickedRecipeUpdatePacket(Recipe<?> recipe) {
        if(recipe == null) {
            this.recipe = null;
        } else {
            this.recipe = recipe.getId();
        }
    }

    public LastClickedRecipeUpdatePacket(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    public static void handle(LastClickedRecipeUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                BteMobsMod.handleLastClickedRecipeUpdatePacket(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }


    public static LastClickedRecipeUpdatePacket decode(FriendlyByteBuf packetBuffer) {
        if(packetBuffer.readableBytes() != 0) {
            return new LastClickedRecipeUpdatePacket(packetBuffer.readResourceLocation());
        } else {
            throw new IllegalArgumentException("LastClickedRecipeUpdatePacket error 112");
        }
    }

    public static void encode(LastClickedRecipeUpdatePacket msg, FriendlyByteBuf packetBuffer) {
        if(msg.recipe != null) {
            packetBuffer.writeResourceLocation(msg.recipe);
        }
    }
}
