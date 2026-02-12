package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.compendium.PagesManager;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.packets.CheckUnlockRecipePacket;
import fr.shoqapik.btemobs.packets.ShowDialogPacket;
import fr.shoqapik.btemobs.packets.SyncUnlockLevelPacket;
import fr.shoqapik.btemobs.quests.Quest;
import fr.shoqapik.btemobs.quests.QuestManager;
import fr.shoqapik.btemobs.rumors.Rumor;
import fr.shoqapik.btemobs.rumors.RumorsManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {
    private static int previousTimesChanged = 0;

    private static final boolean alreadySetToTrue = false;

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(event.side.isClient()) {
            Minecraft.getInstance().player.getRecipeBook().setOpen(BteMobsMod.BLACKSMITH, true);
            Inventory inventory = Minecraft.getInstance().player.getInventory();
            if (inventory.getTimesChanged() != previousTimesChanged) {
                previousTimesChanged = inventory.getTimesChanged();
                BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
            }
        }
        if(event.player instanceof ServerPlayer serverPlayer){
            if(BteMobsMod.unlockLevel== Rumor.UnlockLevel.END)return;
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

    @SubscribeEvent
    public static void entityClickEvent(PlayerInteractEvent.EntityInteract event) {
        if(event.getHand() != InteractionHand.MAIN_HAND) return;
        if(event.getEntity() instanceof ServerPlayer) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getTarget().getType());
            Quest.Type type = Quest.Type.PRESENTATION;
            if(event.getTarget() instanceof BteAbstractEntity && ((BteAbstractEntity)event.getTarget()).getInteractedPlayers().contains(event.getEntity().getUUID())) {
                type = Quest.Type.TASKING;
            }
            Quest quest = QuestManager.getQuest(entityId, type);
            if(quest == null) quest = QuestManager.getQuest(entityId, Quest.Type.TASKING);
            if(quest != null && event.getTarget() instanceof BteAbstractEntity) {
                BteAbstractEntity bteAbstractEntity = (BteAbstractEntity) event.getTarget();
                BteMobsMod.sendToClient(new ShowDialogPacket(event.getTarget().getId(), bteAbstractEntity.getNpcType(), quest), (ServerPlayer) event.getEntity());
                bteAbstractEntity.getInteractedPlayers().add(event.getEntity().getUUID());
            }
        }
    }
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event){
        if (event.getEntity().level.isClientSide){
            BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
        }
    }

    @SubscribeEvent
    public static void addQuestsData(AddReloadListenerEvent event){
        event.addListener(new QuestManager());
        event.addListener(new RumorsManager());
        event.addListener(new PagesManager());

    }

}
