package fr.shoqapik.btemobs.client;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.gui.CompediumScreen;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.packets.CheckUnlockRecipePacket;
import fr.shoqapik.btemobs.packets.SyncUnlockLevelPacket;
import fr.shoqapik.btemobs.rumors.Rumor;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.data.advancements.NetherAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.checkerframework.checker.units.qual.C;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static int previousTimesChanged = 0;
    private static Minecraft minecraft = Minecraft.getInstance();

    private static boolean alreadySetToTrue = false;

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(minecraft.player != null) {
            Minecraft.getInstance().player.getRecipeBook().setOpen(RecipeBookType.valueOf("BLACKSMITH"), true);
            //Minecraft.getInstance().player.getRecipeBook().setOpen(RecipeBookType.valueOf("WARLOCK"), true);
            Inventory inventory = minecraft.player.getInventory();
            if (inventory.getTimesChanged() != previousTimesChanged) {
                previousTimesChanged = inventory.getTimesChanged();
                BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
            }
        }
        if(event.player instanceof ServerPlayer serverPlayer){
            if(BteMobsMod.unlockLevel==Rumor.UnlockLevel.END)return;
            Advancement enterEnd = serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft","end/root"));
            if(enterEnd!=null && serverPlayer.getAdvancements().getOrStartProgress(enterEnd).isDone()){
                BteMobsMod.unlockLevel = Rumor.UnlockLevel.END;
                BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.END;
                BteMobsMod.sendToClient(new SyncUnlockLevelPacket(1),serverPlayer);
                return;
            }
            Advancement enterNether =serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft","nether/root"));
            if(enterNether!=null && serverPlayer.getAdvancements().getOrStartProgress(enterNether).isDone()){
                BteMobsMod.unlockLevel = Rumor.UnlockLevel.NETHER;
                BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.NETHER;
                BteMobsMod.sendToClient(new SyncUnlockLevelPacket(0),serverPlayer);
            }
        }
    }




}
