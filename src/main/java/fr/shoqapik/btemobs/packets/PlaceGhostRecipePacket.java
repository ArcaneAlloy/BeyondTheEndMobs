package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.client.BteMobsModClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaceGhostRecipePacket {
    private final int containerId;
    private final ResourceLocation recipe;

    public PlaceGhostRecipePacket(int pContainerId, Recipe<?> pRecipe) {
        this.containerId = pContainerId;
        this.recipe = pRecipe.getId();
    }

    public PlaceGhostRecipePacket(int pContainerId, ResourceLocation pRecipe) {
        this.containerId = pContainerId;
        this.recipe = pRecipe;
    }

    public PlaceGhostRecipePacket(FriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readByte();
        this.recipe = pBuffer.readResourceLocation();
    }

    public static void encode(PlaceGhostRecipePacket msg, FriendlyByteBuf packetBuffer) {
        if(msg.recipe!=null){
            packetBuffer.writeByte(msg.containerId);
            packetBuffer.writeUtf(msg.recipe.toString());
        }
    }

    public static PlaceGhostRecipePacket decode(FriendlyByteBuf packetBuffer) {
        if(packetBuffer.readableBytes()!=0){
            return new PlaceGhostRecipePacket(packetBuffer.readByte(), packetBuffer.readResourceLocation());
        }else {
            return new PlaceGhostRecipePacket(-1, (ResourceLocation) null);
        }
    }

    public static void handle(PlaceGhostRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                BteMobsModClient.handlePlaceGhostRecipe(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }

    public ResourceLocation getRecipe() {
        return this.recipe;
    }

    public int getContainerId() {
        return this.containerId;
    }
}
