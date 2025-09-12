package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.entity.ItemPart;
import fr.shoqapik.btemobs.menu.TableExplorerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartCraftingItemPacket {

    public ItemStack result;
    public int id;

    public StartCraftingItemPacket(ItemStack result,int id) {
        this.result = result;
        this.id = id;
    }

    public static void handle(StartCraftingItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->{
            if(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT){
                assert Minecraft.getInstance().level!=null;
                Entity entity = Minecraft.getInstance().level.getEntity(msg.id);
                if(entity instanceof ItemPart part){
                    part.item=msg.result;
                }
            }else {
                handlePlaceRecipePacket(msg, ctx);
            }

        }
        );
        ctx.get().setPacketHandled(true);
    }

    public static void handlePlaceRecipePacket(StartCraftingItemPacket msg, Supplier<NetworkEvent.Context> ctx){
        Entity entity = ctx.get().getSender().level.getEntity(msg.id);
        if(entity instanceof ExplorerEntity explorer){
            explorer.startCrafting(msg.result);
        }
        if(entity instanceof DruidEntity druid){
            druid.startCrafting(msg.result);
            druid.clearContent();
        }

        if(ctx.get().getSender().containerMenu instanceof TableExplorerMenu menu){
            menu.craftSlots.items.clear();
        }
    }


    public static StartCraftingItemPacket decode(FriendlyByteBuf packetBuffer) {
        ItemStack recipe = packetBuffer.readItem();
        int id = packetBuffer.readInt();
        return new StartCraftingItemPacket(recipe,id);
    }

    public static void encode(StartCraftingItemPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeItem(msg.result);
        packetBuffer.writeInt(msg.id);
    }



}
