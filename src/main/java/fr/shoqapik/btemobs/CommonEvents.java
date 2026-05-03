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

        // FIX: Resetear BLACKSMITH en el NBT del servidor al hacer login.
        // Minecraft inicializa RecipeBookType personalizados con isOpen=true por defecto.
        // Al resetear aqui en el servidor, el cliente recibe el estado correcto (false)
        // antes de que se renderice cualquier pantalla.
        serverPlayer.getRecipeBook().setOpen(BteMobsMod.BLACKSMITH, false);
    }

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(event.side.isClient()) {
            if (Minecraft.getInstance().player != null) {
                boolean currentlyOpen = Minecraft.getInstance().player.getRecipeBook().isOpen(BteMobsMod.BLACKSMITH);
                if (!openedOnce) {
                    Minecraft.getInstance().player.getRecipeBook().setOpen(BteMobsMod.BLACKSMITH, false);
                    openedOnce = true;
                } else if (currentlyOpen) {
                    Minecraft.getInstance().player.getRecipeBook().setOpen(BteMobsMod.BLACKSMITH, false);
                }
            }
            // FIX: Si el libro de recetas se abre automaticamente sin que haya
            // ninguna pantalla de NPC abierta, cerrarlo inmediatamente.
            // Esto ocurre cuando awardRecipes llega al cliente y Minecraft
            // abre el libro para mostrar las recetas nuevas.
            net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
            boolean isNpcScreen = currentScreen instanceof fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen
                || currentScreen instanceof fr.shoqapik.btemobs.client.gui.WarlockCraftScreen
                || currentScreen instanceof fr.shoqapik.btemobs.client.gui.ExplorerTableScreen
                || currentScreen instanceof fr.shoqapik.btemobs.client.gui.DruidScreen
                || currentScreen instanceof fr.shoqapik.btemobs.client.gui.WarlockPotionCraftScreen;
            if (!isNpcScreen) {
                net.minecraft.client.ClientRecipeBook recipeBook = Minecraft.getInstance().player.getRecipeBook();
                for (net.minecraft.world.inventory.RecipeBookType type : net.minecraft.world.inventory.RecipeBookType.values()) {
                    if (recipeBook.isOpen(type)) {
                        recipeBook.setOpen(type, false);
                    }
                }
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

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof net.minecraft.world.item.EnchantedBookItem)) return;

        // Buscar si hay una WarlockRecipe que requiera este libro encantado
        java.util.List<fr.shoqapik.btemobs.recipe.WarlockRecipe> warlockRecipes =
            BteMobsMod.getServer().getRecipeManager().getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_RECIPE.get());

        for (fr.shoqapik.btemobs.recipe.WarlockRecipe recipe : warlockRecipes) {
            fr.shoqapik.btemobs.UnlockRecipe unlockRecipe = ServerData.get().getUnlockRecipe(recipe);

            // Si ya está desbloqueada, mostrar mensaje y salir
            if (ServerData.get().isUnlock(recipe)) {
                event.setCanceled(true);
                net.minecraft.network.chat.Component enchantName = net.minecraft.network.chat.Component.translatable(
                    recipe.getEnchantment().getDescriptionId())
                    .append(" ")
                    .append(net.minecraft.network.chat.Component.translatable("enchantment.level." + recipe.getLevel()));
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.already_unlocked",
                        enchantName)
                        .withStyle(net.minecraft.ChatFormatting.GREEN),
                    false
                );
                return;
            }

            // Comprobar si este libro corresponde a esta receta
            if (!unlockRecipe.is(stack)) continue;

            // Es el libro correcto - cancelar el evento para evitar uso normal del libro
            event.setCanceled(true);

            mc.duzo.ender_journey.capabilities.PortalPlayer portalPlayer =
                mc.duzo.ender_journey.capabilities.PortalPlayer.get(player).orElse(null);
            if (portalPlayer == null) return;

            int eyesEarned = portalPlayer.getEyesEarn();
            int eyesNeeded = recipe.getNeedEyes();

            if (eyesEarned >= eyesNeeded) {
                // Tiene suficientes ojos - desbloquear receta y consumir el libro
                unlockRecipe.setWasFound(true);
                unlockRecipe.setIsLock(false);

                java.util.List<net.minecraft.world.item.crafting.Recipe<?>> toUnlock = new java.util.ArrayList<>();
                toUnlock.add(recipe);
                net.minecraft.advancements.CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipe);
                BteMobsMod.addRecipe(player, fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_RECIPE.get(), toUnlock);

                // Consumir el libro del inventario
                stack.shrink(1);

                // Mensaje de éxito en el chat
                net.minecraft.network.chat.Component enchantName2 = net.minecraft.network.chat.Component.translatable(
                    recipe.getEnchantment().getDescriptionId())
                    .append(" ")
                    .append(net.minecraft.network.chat.Component.translatable("enchantment.level." + recipe.getLevel()));
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.unlocked",
                        enchantName2)
                        .withStyle(net.minecraft.ChatFormatting.GREEN),
                    false
                );
            } else {
                // No tiene suficientes ojos - mensaje en overlay y chat
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.locked",
                        eyesNeeded)
                        .withStyle(net.minecraft.ChatFormatting.RED),
                    true  // overlay (action bar)
                );
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.locked_chat",
                        eyesNeeded)
                        .withStyle(net.minecraft.ChatFormatting.RED),
                    false  // chat
                );
            }
            return;
        }
    }
}
