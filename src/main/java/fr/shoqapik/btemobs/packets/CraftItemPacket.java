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
    public final boolean skipAnimation;

    public CraftItemPacket(Recipe<?> recipe, boolean skipAnimation) {
        this(recipe == null ? null : recipe.getId(), skipAnimation);
    }

    public CraftItemPacket(ResourceLocation recipe, boolean skipAnimation) {
        this.recipe = recipe;
        this.skipAnimation = skipAnimation;
    }

    public static void handle(CraftItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                BteMobsMod.handleCraftItemPacket(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }


    public static CraftItemPacket decode(FriendlyByteBuf packetBuffer) {
        ResourceLocation location = packetBuffer.readBoolean() ? packetBuffer.readResourceLocation() : null;
        boolean skipAnimation = packetBuffer.readBoolean();
        return new CraftItemPacket(location, skipAnimation);
    }

    public static void encode(CraftItemPacket msg, FriendlyByteBuf packetBuffer) {
        if (msg.recipe != null) {
            packetBuffer.writeBoolean(true);
            packetBuffer.writeResourceLocation(msg.recipe);
        } else {
            packetBuffer.writeBoolean(false);
        }
        packetBuffer.writeBoolean(msg.skipAnimation);
    }
}
