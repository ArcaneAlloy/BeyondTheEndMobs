package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.capability.BteCapability;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.compendium.PagesManager;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.packets.CheckUnlockRecipePacket;
import fr.shoqapik.btemobs.packets.ShowDialogPacket;
import fr.shoqapik.btemobs.packets.SyncRecipeManager;
import fr.shoqapik.btemobs.packets.SyncUnlockLevelPacket;
import fr.shoqapik.btemobs.quests.Quest;
import fr.shoqapik.btemobs.quests.QuestManager;
import fr.shoqapik.btemobs.rumors.Rumor;
import fr.shoqapik.btemobs.rumors.RumorsManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {
    private static int previousTimesChanged = 0;
    private static boolean openedOnce = false;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SubscribeEvent
    public static void attachEntityCapability(AttachCapabilitiesEvent<Entity> event) {
        if(event.getObject() instanceof Player player){
            RecipeCapability oldCap = RecipeCapability.get(player);
            if (oldCap == null) {
                RecipeCapability.RecipeProvider prov = new RecipeCapability.RecipeProvider();
                RecipeCapability cap = prov.getCapability(BteCapability.RECIPE_CAPABILITY).orElse(null);
                cap.init(player, player.level);
                event.addCapability(new ResourceLocation(BteMobsMod.MODID, "multi_arm_cap"), prov);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Player player){
            RecipeCapability cap = RecipeCapability.get(player);
            if(cap != null && event.getEntity().isAlive()){
                cap.tick((Player) event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if(event.getEntity().level.isClientSide) return;
        if (!event.isWasDeath()) return;

        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        oldPlayer.reviveCaps();

        RecipeCapability oldCap = RecipeCapability.get(oldPlayer);
        if(oldCap != null){
            RecipeCapability cap = RecipeCapability.get(newPlayer);
            cap.init(newPlayer, newPlayer.level);
            cap.copyFrom(oldCap);
            BteMobsMod.sendToClient(new SyncRecipeManager(newPlayer.getId(), cap.serializeNBT(), event.isWasDeath()), (ServerPlayer) newPlayer);
        }
        oldPlayer.invalidateCaps();
    }

    /**
     * BUG FIX: Sincronizar el unlock level al cliente cuando el jugador hace login.
     * En multiplayer, el SyncUnlockLevelPacket solo se enviaba al ganar el avance,
     * así que jugadores que se reconectan o se unen después nunca lo recibían.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        int unlockId = getUnlockIdForPlayer(serverPlayer);
        BteMobsMod.sendToClient(new SyncUnlockLevelPacket(unlockId), serverPlayer);
    }

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(event.side.isClient()) {
            if (!openedOnce && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getRecipeBook().setOpen(BteMobsMod.BLACKSMITH, true);
                openedOnce = true;
            }
            Inventory inventory = Minecraft.getInstance().player.getInventory();
            if (inventory.getTimesChanged() != previousTimesChanged) {
                previousTimesChanged = inventory.getTimesChanged();
                BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
            }
        }

        if(event.player instanceof ServerPlayer serverPlayer){
            // BUG FIX: Calcular el unlock level para ESTE jugador específico,
            // no usar el campo estático global que se sobreescribe entre jugadores.
            int currentUnlockId = getUnlockIdForPlayer(serverPlayer);

            // Solo enviar si el nivel cambió respecto al estado actual del cliente
            // Para evitar spam, usamos el campo estático solo como caché del servidor
            // y enviamos al jugador individual (no a todos)
            Advancement enterEnd = serverPlayer.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("minecraft", "end/root"));
            if(enterEnd != null && serverPlayer.getAdvancements().getOrStartProgress(enterEnd).isDone()) {
                if(BteMobsMod.unlockLevel != Rumor.UnlockLevel.END) {
                    BteMobsMod.unlockLevel = Rumor.UnlockLevel.END;
                    BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.END;
                }
                // Siempre sincronizar al jugador individual por si es nuevo o se reconectó
                BteMobsMod.sendToClient(new SyncUnlockLevelPacket(1), serverPlayer);
                return;
            }
            Advancement enterNether = serverPlayer.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("minecraft", "nether/root"));
            if(enterNether != null && serverPlayer.getAdvancements().getOrStartProgress(enterNether).isDone()) {
                BteMobsMod.sendToClient(new SyncUnlockLevelPacket(0), serverPlayer);
            }
        }
    }

    /**
     * Determina el unlock level de un jugador basándose en sus avances.
     * -1 = OVERWORLD, 0 = NETHER, 1 = END
     */
    private static int getUnlockIdForPlayer(ServerPlayer player) {
        Advancement enterEnd = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation("minecraft", "end/root"));
        if(enterEnd != null && player.getAdvancements().getOrStartProgress(enterEnd).isDone()) {
            return 1;
        }
        Advancement enterNether = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation("minecraft", "nether/root"));
        if(enterNether != null && player.getAdvancements().getOrStartProgress(enterNether).isDone()) {
            return 0;
        }
        return -1; // OVERWORLD
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
