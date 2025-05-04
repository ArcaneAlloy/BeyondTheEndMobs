package fr.shoqapik.btemobs.client;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.packets.CheckUnlockRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static int previousTimesChanged = 0;
    private static Minecraft minecraft = Minecraft.getInstance();

    private static boolean alreadySetToTrue = false;

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(minecraft.player != null) {
            Minecraft.getInstance().player.getRecipeBook().setOpen(RecipeBookType.valueOf("BLACKSMITH"), true);
            Minecraft.getInstance().player.getRecipeBook().setOpen(RecipeBookType.valueOf("WARLOCK"), true);

            Inventory inventory = minecraft.player.getInventory();
            if (inventory.getTimesChanged() != previousTimesChanged) {
                previousTimesChanged = inventory.getTimesChanged();
                BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
            }
        }
    }




}
