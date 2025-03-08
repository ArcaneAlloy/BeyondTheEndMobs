package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.menu.container.explorer_menu.TableExplorerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaceItemRecipePacket {

    public ItemStack result;

    public PlaceItemRecipePacket(ItemStack result) {
        this.result = result;
    }

    public static void handle(PlaceItemRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                handlePlaceRecipePacket(msg, ctx)
        );
        ctx.get().setPacketHandled(true);
    }

    public static void handlePlaceRecipePacket(PlaceItemRecipePacket msg, Supplier<NetworkEvent.Context> ctx){
        if(ctx.get().getSender().containerMenu instanceof TableExplorerMenu){
            TableExplorerMenu menu = (TableExplorerMenu) ctx.get().getSender().containerMenu;
            menu.placeRecipe(ctx.get().getSender(), msg.result);
        }
    }


    public static PlaceItemRecipePacket decode(FriendlyByteBuf packetBuffer) {
        ItemStack recipe = packetBuffer.readItem();
        return new PlaceItemRecipePacket(recipe);
    }

    public static void encode(PlaceItemRecipePacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeItem(msg.result);
    }



}
