package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.rumors.Rumor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncUnlockLevelPacket(int id) {

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(this::handlePlaceRecipePacket);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public void handlePlaceRecipePacket() {
        if (id == 0) {
            BteMobsMod.unlockLevel = Rumor.UnlockLevel.NETHER;
            BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.NETHER;

        } else if (id == 1) {
            BteMobsMod.unlockLevel = Rumor.UnlockLevel.END;
            BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.END;

        } else {
            BteMobsMod.unlockLevel = Rumor.UnlockLevel.OVERWORLD;
            BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.OVERWORLD;
        }
    }

    public static SyncUnlockLevelPacket decode(FriendlyByteBuf packetBuffer) {
        return new SyncUnlockLevelPacket(packetBuffer.readInt());
    }

    public static void encode(SyncUnlockLevelPacket msg, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeInt(msg.id);
    }

}
