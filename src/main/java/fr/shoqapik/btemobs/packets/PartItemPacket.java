package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ItemPart;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PartItemPacket {

    public ItemStack result;
    public int id;
    public String name;
    public int extraTime;
    public PartItemPacket(ItemStack result, int id,int extraTime,String part) {
        this.result = result;
        this.id = id;
        this.name = part;
        this.extraTime = extraTime;
    }

    public static void handle(PartItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->{
                    assert Minecraft.getInstance().level!=null;
                    Entity entity = Minecraft.getInstance().level.getEntity(msg.id);
                    if(entity instanceof DruidEntity druid){
                        ItemPart part=null;
                        for (ItemPart part1 : druid.samplesItems){
                            if(part1.name.equals(msg.name)){
                                part = part1;
                                break;
                            }
                        }
                        if (part!=null){
                            part.item = msg.result;
                            part.maxSampleTime = 200 + msg.extraTime;
                        }
                    }

        }
        );
        ctx.get().setPacketHandled(true);
    }


    public static PartItemPacket decode(FriendlyByteBuf packetBuffer) {
        ItemStack recipe = packetBuffer.readItem();
        int id = packetBuffer.readInt();
        int extraTime = packetBuffer.readInt();
        String name = packetBuffer.readUtf();
        return new PartItemPacket(recipe,id,extraTime,name);
    }

    public static void encode(PartItemPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeItem(msg.result);
        packetBuffer.writeInt(msg.id);
        packetBuffer.writeInt(msg.extraTime);
        packetBuffer.writeUtf(msg.name);
    }



}
