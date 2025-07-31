package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.packets.ShowDialogPacket;
import fr.shoqapik.btemobs.quests.Quest;
import fr.shoqapik.btemobs.quests.QuestManager;
import fr.shoqapik.btemobs.quests.Rumor;
import fr.shoqapik.btemobs.quests.RumorsManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

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
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if(event.getItemStack().is(Items.STICK)){
            if(event.getEntity().isShiftKeyDown()){
            }else {
                BteMobsMod.x-=0.5D;
            }
            BteMobsMod.LOGGER.debug("X :" + BteMobsMod.x);
        }

        if(event.getItemStack().is(Items.BLAZE_ROD)){
            if(event.getEntity().isShiftKeyDown()){
            }else {
                BteMobsMod.y-=0.5D;
            }
            BteMobsMod.LOGGER.debug("Y :" + BteMobsMod.y);
        }
        if(event.getItemStack().is(Items.PRISMARINE_SHARD)){
            BteMobsMod.y+=0.5D;

            BteMobsMod.LOGGER.debug("Z :" + BteMobsMod.z);
        }

        if(event.getItemStack().is(Items.HEART_OF_THE_SEA)){
            if(event.getEntity().isShiftKeyDown()){
            }else {
                BteMobsMod.x+=0.5D;
            }
            BteMobsMod.LOGGER.debug("XQ :" + BteMobsMod.xq);
        }
        if(event.getItemStack().is(Items.GOLD_INGOT)){
            BteMobsMod.unlockLevel = Rumor.UnlockLevel.NETHER;
        }

        if(event.getItemStack().is(Items.NETHERITE_INGOT)){
            BteMobsMod.unlockLevel = Rumor.UnlockLevel.END;
        }
        BteMobsMod.LOGGER.debug("X :" + BteMobsMod.x + " Y :"+BteMobsMod.y+
                " Z :" + BteMobsMod.z + " XQ :"+BteMobsMod.xq+
                " YQ :" + BteMobsMod.yq + " ZQ :"+BteMobsMod.zq);

    }
    @SubscribeEvent
    public static void addQuestsData(AddReloadListenerEvent event){
        event.addListener(new QuestManager());
        event.addListener(new RumorsManager());
    }

}
