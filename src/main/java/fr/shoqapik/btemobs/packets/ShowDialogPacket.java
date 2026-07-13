package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.client.BteMobsModClient;
import fr.shoqapik.btemobs.client.gui.QuestScreen;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.option_dialogs.OptionDialogs;
import fr.shoqapik.btemobs.quest.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShowDialogPacket {
    public int entityId;
    public BteNpcType bteNpcType;
    public OptionDialogs quest;

    public ShowDialogPacket(int entityId, BteNpcType bteNpcType, OptionDialogs quest) {
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quest = quest;
    }

    public static void handle(ShowDialogPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->{
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BteMobsModClient.handleDialogPacket(msg, ctx));
                }
        );
        ctx.get().setPacketHandled(true);
    }


    public static ShowDialogPacket decode(FriendlyByteBuf packetBuffer) {
        int entityId = packetBuffer.readInt();
        String bteNpcType = packetBuffer.readUtf();
        OptionDialogs quest = OptionDialogs.decode(packetBuffer);
        return new ShowDialogPacket(entityId, BteNpcType.valueOf(bteNpcType), quest);
    }

    public static void encode(ShowDialogPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(msg.entityId);
        packetBuffer.writeUtf(msg.bteNpcType.name());
        OptionDialogs.encode(msg.quest, packetBuffer);
    }

}
