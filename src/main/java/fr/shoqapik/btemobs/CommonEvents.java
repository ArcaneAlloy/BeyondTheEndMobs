package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.capability.BteCapability;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.StatRewardData;
import fr.shoqapik.btemobs.client.gui.QuestScreen;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.compendium.PagesManager;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.packets.CheckUnlockRecipePacket;
import fr.shoqapik.btemobs.packets.ShowDialogPacket;
import fr.shoqapik.btemobs.packets.SyncRecipeManager;
import fr.shoqapik.btemobs.packets.SyncUnlockLevelPacket;
import fr.shoqapik.btemobs.option_dialogs.OptionDialogs;
import fr.shoqapik.btemobs.option_dialogs.OptionDialogsManager;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestManager;
import fr.shoqapik.btemobs.rumors.Rumor;
import fr.shoqapik.btemobs.rumors.RumorsManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

import static fr.shoqapik.btemobs.SacredPlaceHandler.FORGOTTEN_REALM;

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
    public static void onDeath(LivingDeathEvent event){
        if (event.getSource().getEntity() instanceof Player player){
            RecipeCapability cap = RecipeCapability.get(player);
            if (cap != null){
                cap.hunterQuestUpdate(event);
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

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        int unlockId = getUnlockIdForPlayer(serverPlayer);
        BteMobsMod.sendToClient(new SyncUnlockLevelPacket(unlockId), serverPlayer);

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

            int currentUnlockId = getUnlockIdForPlayer(serverPlayer);

            Advancement enterEnd = serverPlayer.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("minecraft", "end/root"));
            if(enterEnd != null && serverPlayer.getAdvancements().getOrStartProgress(enterEnd).isDone()) {
                if(BteMobsMod.unlockLevel != Rumor.UnlockLevel.END) {
                    BteMobsMod.unlockLevel = Rumor.UnlockLevel.END;
                    BteMobsMod.unlockLevel1 = PageCompendium.UnlockLevel.END;
                }
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
            OptionDialogs.Type type = OptionDialogs.Type.PRESENTATION;
            if(event.getTarget() instanceof BteAbstractEntity && ((BteAbstractEntity)event.getTarget()).getInteractedPlayers().contains(event.getEntity().getUUID())) {
                type = OptionDialogs.Type.TASKING;
            }
            OptionDialogs quest = OptionDialogsManager.getQuest(entityId, type);
            if(quest == null) quest = OptionDialogsManager.getQuest(entityId, OptionDialogs.Type.TASKING);
            if(quest != null && event.getTarget() instanceof BteAbstractEntity) {
                BteAbstractEntity bteAbstractEntity = (BteAbstractEntity) event.getTarget();
                UUID playerUuid = event.getEntity().getUUID();

                boolean muteSound = false;
                if (quest.getType() == OptionDialogs.Type.TASKING) {
                    if (bteAbstractEntity.hasTaskingSoundCounter(playerUuid)) {
                        // El contador de este NPC ya esta en 1: no repetir el sonido.
                        muteSound = true;
                    } else {
                        // Segunda vez que habla con este NPC: contador 0 -> 1.
                        bteAbstractEntity.markTaskingSoundPlayed(playerUuid);
                    }
                }

                BteMobsMod.sendToClient(new ShowDialogPacket(event.getTarget().getId(), bteAbstractEntity.getNpcType(), quest, muteSound), (ServerPlayer) event.getEntity());
                bteAbstractEntity.getInteractedPlayers().add(playerUuid);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Al salir del lobby (ender_journey:the_forgotten_realm), se reinicia
        // a 0 el contador de sonido "tasking" de cada NPC para ese jugador.
        if (!event.getFrom().equals(FORGOTTEN_REALM)) return;
        if (event.getTo().equals(FORGOTTEN_REALM)) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ServerLevel forgottenRealmLevel = serverPlayer.getServer().getLevel(FORGOTTEN_REALM);
        if (forgottenRealmLevel == null) return;

        UUID playerUuid = serverPlayer.getUUID();
        for (Entity entity : forgottenRealmLevel.getAllEntities()) {
            if (entity instanceof BteAbstractEntity bteAbstractEntity) {
                bteAbstractEntity.resetTaskingSoundCounter(playerUuid);
            }
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event){
        if (event.getEntity().level.isClientSide){
            BteMobsMod.sendToServer(new CheckUnlockRecipePacket());
        }
    }


    private static java.util.Map<String, Integer> ITEM_TIERS = null;

    public static java.util.Map<String, Integer> getItemTiers() {
        if (ITEM_TIERS != null) return ITEM_TIERS;
        ITEM_TIERS = new java.util.HashMap<>();
        try {
            java.io.InputStream is = CommonEvents.class.getResourceAsStream("/data/bte_mobs/item_tiers.json");
            if (is != null) {
                String json = new String(is.readAllBytes());
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                for (var entry : obj.entrySet()) {
                    ITEM_TIERS.put(entry.getKey(), entry.getValue().getAsInt());
                }
            }
        } catch (Exception e) {
            BteMobsMod.LOGGER.error("Failed to load item_tiers.json", e);
        }
        return ITEM_TIERS;
    }

    public static net.minecraft.ChatFormatting getTierStyle(int tier) {
        return switch (tier) {
            case 0 -> net.minecraft.ChatFormatting.WHITE;
            case 1 -> net.minecraft.ChatFormatting.GREEN;
            case 2 -> net.minecraft.ChatFormatting.AQUA;
            case 3 -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            case 4 -> net.minecraft.ChatFormatting.GOLD;
            case 5 -> net.minecraft.ChatFormatting.RED;
            default -> net.minecraft.ChatFormatting.GRAY;
        };
    }

    // ── Tooltip de uso por NPC (Anna/Blacksmith, Antonio/Explorer, Oriana/Druid, Noah/Warlock) ──
    // Mapa: item id -> lista de tags "crudos", RECONSTRUIDO EN VIVO a partir del RecipeManager
    // cada vez que las recetas se sincronizan/recargan (ver rebuildItemUsage() mas abajo y el
    // handler de RecipesUpdatedEvent en ClientForgeEvents). Ya no depende de ningun archivo
    // generado a mano: anadir/editar/borrar una receta bte_mobs:* se refleja solo. Tags posibles:
    //   CRAFTED_BLACKSMITH  - el item es resultado de una receta de Anna (blacksmith / upgrade)
    //   CRAFTED_EXPLORER    - el item es resultado de una receta de Antonio (explorer)
    //   CRAFTED_DRUID       - el item es resultado de una receta de Oriana (druid)
    //   UPGRADING           - el item es la base de una mejora en la Blacksmith
    //   MATERIAL_CRAFTING   - el item es un ingrediente (no base) de blacksmith/upgrade/explorer/druid
    //   ENCHANTING          - el item es material para encantamientos de Noah (warlock)
    //   POTION              - el item es material para pociones de Noah (warlock), incluye redstone,
    //                         glowstone y dragon's breath
    // Para el tooltip, esos tags crudos se agrupan en 4 categorias de display (una sola linea
    // "Used By: X, Y, Z"): CRAFTING (cualquier CRAFTED_* o MATERIAL_CRAFTING), UPGRADING, ENCHANTING, POTION.
    private static volatile java.util.Map<String, java.util.List<String>> ITEM_USAGE_CACHE = java.util.Collections.emptyMap();

    private static final java.util.List<String> USAGE_DISPLAY_GROUP_ORDER = java.util.List.of(
        "CRAFTING", "UPGRADING", "ENCHANTING", "POTION"
    );

    public static java.util.List<String> getUsageDisplayGroupOrder() {
        return USAGE_DISPLAY_GROUP_ORDER;
    }

    public static String getUsageDisplayGroup(String rawTag) {
        return switch (rawTag) {
            case "CRAFTED_BLACKSMITH", "CRAFTED_EXPLORER", "CRAFTED_DRUID", "MATERIAL_CRAFTING" -> "CRAFTING";
            case "UPGRADING" -> "UPGRADING";
            case "ENCHANTING" -> "ENCHANTING";
            case "POTION" -> "POTION";
            default -> null;
        };
    }

    public static java.util.Map<String, java.util.List<String>> getItemUsage() {
        return ITEM_USAGE_CACHE;
    }

    /**
     * Reconstruye el mapa de uso a partir del RecipeManager actual (recetas ya sincronizadas al
     * cliente). Se llama desde RecipesUpdatedEvent, asi que se ejecuta sola al entrar a un mundo
     * y cada vez que se recargan datapacks (/reload) - no hace falta generar nada a mano nunca.
     */
    public static void rebuildItemUsage(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
        java.util.Map<String, java.util.Set<String>> usage = new java.util.HashMap<>();

        java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, String> tagStack = (stack, tag) -> {
            if (stack == null || stack.isEmpty()) return;
            net.minecraft.resources.ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null) return;
            usage.computeIfAbsent(rl.toString(), k -> new java.util.LinkedHashSet<>()).add(tag);
        };

        java.util.function.BiConsumer<net.minecraft.world.item.crafting.Ingredient, String> tagIngredient = (ingredient, tag) -> {
            if (ingredient == null) return;
            for (net.minecraft.world.item.ItemStack stack : ingredient.getItems()) {
                tagStack.accept(stack, tag);
            }
        };

        for (fr.shoqapik.btemobs.recipe.BlacksmithRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.BLACKSMITH_RECIPE.get())) {
            tagStack.accept(recipe.getResultItem(), "CRAFTED_BLACKSMITH");
            for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
                tagIngredient.accept(ing, "MATERIAL_CRAFTING");
            }
        }

        for (fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get())) {
            tagStack.accept(recipe.getResultItem(), "CRAFTED_BLACKSMITH");
            tagIngredient.accept(recipe.base, "UPGRADING");
            for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
                if (ing == recipe.base) continue; // ya etiquetado como UPGRADING, no como material generico
                tagIngredient.accept(ing, "MATERIAL_CRAFTING");
            }
        }

        for (fr.shoqapik.btemobs.recipe.ExplorerRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get())) {
            tagStack.accept(recipe.getResultItem(), "CRAFTED_EXPLORER");
            tagIngredient.accept(recipe.getRequiredItems(), "MATERIAL_CRAFTING");
        }

        for (fr.shoqapik.btemobs.recipe.api.DruidRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get())) {
            tagStack.accept(recipe.getResultItem(), "CRAFTED_DRUID");
            tagIngredient.accept(recipe.getRequiredItems(), "MATERIAL_CRAFTING");
        }

        for (fr.shoqapik.btemobs.recipe.WarlockRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_RECIPE.get())) {
            tagIngredient.accept(recipe.getRequiredItems(), "ENCHANTING");
        }

        for (fr.shoqapik.btemobs.recipe.WarlockPotionRecipe recipe :
                recipeManager.getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get())) {
            tagStack.accept(recipe.getIngredientPrimary(), "POTION");
        }

        // Modificadores universales de pociones: estan hardcodeados en WarlockPotionRecipe#matches
        // (redstone/glowstone/dragon's breath en slots fijos), no aparecen en el JSON de cada receta.
        usage.computeIfAbsent("minecraft:redstone", k -> new java.util.LinkedHashSet<>()).add("POTION");
        usage.computeIfAbsent("minecraft:glowstone_dust", k -> new java.util.LinkedHashSet<>()).add("POTION");
        usage.computeIfAbsent("minecraft:dragon_breath", k -> new java.util.LinkedHashSet<>()).add("POTION");

        java.util.Map<String, java.util.List<String>> built = new java.util.HashMap<>();
        for (var entry : usage.entrySet()) {
            built.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
        }
        ITEM_USAGE_CACHE = built;
        BteMobsMod.LOGGER.info("[bte_mobs] Item usage tooltip cache rebuilt: {} items tagged", built.size());
    }

    public static net.minecraft.ChatFormatting getUsageGroupStyle(String group) {
        return switch (group) {
            case "CRAFTING" -> net.minecraft.ChatFormatting.GOLD;
            case "UPGRADING" -> net.minecraft.ChatFormatting.YELLOW;
            case "ENCHANTING" -> net.minecraft.ChatFormatting.AQUA;
            case "POTION" -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            default -> net.minecraft.ChatFormatting.GRAY;
        };
    }

    public static String getUsageGroupLangKey(String group) {
        return switch (group) {
            case "CRAFTING" -> "bte_mobs.usage.crafting";
            case "UPGRADING" -> "bte_mobs.usage.upgrading";
            case "ENCHANTING" -> "bte_mobs.usage.enchanting";
            case "POTION" -> "bte_mobs.usage.potion";
            default -> "bte_mobs.usage.crafting";
        };
    }

    @SubscribeEvent
    public static void addQuestsData(AddReloadListenerEvent event){
        event.addListener(new OptionDialogsManager());
        event.addListener(new QuestManager());
        event.addListener(new RumorsManager());
        event.addListener(new PagesManager());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (stack.getItem() == Items.STICK){
            if (!player.isShiftKeyDown()){
                BteMobsMod.x += 1;
            }else {
                BteMobsMod.x -= 1;
            }
        }
        if (stack.getItem() == Items.GOLD_INGOT){
            if (!player.isShiftKeyDown()){
                BteMobsMod.y += 1;
            }else {
                BteMobsMod.y -= 1;
            }
        }
        if (stack.getItem() == Items.BLAZE_ROD){
            if (!player.isShiftKeyDown()){
                BteMobsMod.xp += 1;
            }else {
                BteMobsMod.xp -= 1;
            }
        }
        if (stack.getItem() == Items.IRON_INGOT){
            if (!player.isShiftKeyDown()){
                BteMobsMod.yp += 1;
            }else {
                BteMobsMod.yp -= 1;
            }
        }
        BteMobsMod.LOGGER.info("X :{} , Y :{} , XP :{} , YP :{}",BteMobsMod.x,BteMobsMod.y,BteMobsMod.xp,BteMobsMod.yp);
        boolean isEnchantedBook = stack.getItem() instanceof net.minecraft.world.item.EnchantedBookItem;
        boolean isAncientTome = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()) != null
            && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("quark:ancient_tome");
        if (!isEnchantedBook && !isAncientTome) return;

        // Obtener todos los encantamientos del libro
        net.minecraft.nbt.ListTag storedEnchants = net.minecraft.world.item.EnchantedBookItem.getEnchantments(stack);
        if (storedEnchants.isEmpty()) return;

        // Si tiene más de un encantamiento → separar en libros individuales
        if (storedEnchants.size() > 1) {
            event.setCanceled(true);
            stack.shrink(1);
            for (int i = 0; i < storedEnchants.size(); i++) {
                net.minecraft.nbt.CompoundTag tag = storedEnchants.getCompound(i);
                net.minecraft.world.item.enchantment.Enchantment ench =
                    net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(
                        new net.minecraft.resources.ResourceLocation(tag.getString("id")));
                if (ench == null) continue;
                net.minecraft.world.item.ItemStack singleBook =
                    net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(ench, tag.getShort("lvl")));
                // Intentar añadir al inventario, si no cabe cae al suelo
                if (!player.getInventory().add(singleBook)) {
                    player.drop(singleBook, false);
                }
            }
            return;
        }

        // Libro con un solo encantamiento → sistema de desbloqueo
        java.util.List<fr.shoqapik.btemobs.recipe.WarlockRecipe> warlockRecipes =
            BteMobsMod.getServer().getRecipeManager().getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_RECIPE.get());

        for (fr.shoqapik.btemobs.recipe.WarlockRecipe recipe : warlockRecipes) {
            fr.shoqapik.btemobs.UnlockRecipe unlockRecipe = ServerData.get().getUnlockRecipe(recipe);
            if (unlockRecipe == null) continue;
            if (!unlockRecipe.is(stack)) continue;

            event.setCanceled(true);

            if (ServerData.get().isUnlock(recipe)) {
                net.minecraft.network.chat.Component enchantName = net.minecraft.network.chat.Component.translatable(
                    recipe.getEnchantment().getDescriptionId())
                    .append(" ")
                    .append(net.minecraft.network.chat.Component.translatable("enchantment.level." + recipe.getLevel()));
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.already_unlocked",
                        enchantName).withStyle(net.minecraft.ChatFormatting.GREEN), false);
                return;
            }

            mc.duzo.ender_journey.capabilities.PortalPlayer portalPlayer =
                mc.duzo.ender_journey.capabilities.PortalPlayer.get(player).orElse(null);
            if (portalPlayer == null) return;

            int eyesEarned = portalPlayer.getEyesEarn();
            int eyesNeeded = recipe.getNeedEyes();

            if (eyesEarned >= eyesNeeded) {
                unlockRecipe.setWasFound(true);
                unlockRecipe.setIsLock(false);
                java.util.List<net.minecraft.world.item.crafting.Recipe<?>> toUnlock = new java.util.ArrayList<>();
                toUnlock.add(recipe);
                net.minecraft.advancements.CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipe);
                BteMobsMod.addRecipe(player, fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_RECIPE.get(), toUnlock);
                stack.shrink(1);
                net.minecraft.network.chat.Component enchantName2 = net.minecraft.network.chat.Component.translatable(
                    recipe.getEnchantment().getDescriptionId())
                    .append(" ")
                    .append(net.minecraft.network.chat.Component.translatable("enchantment.level." + recipe.getLevel()));
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.unlocked",
                        enchantName2).withStyle(net.minecraft.ChatFormatting.GREEN), false);
            } else {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("bte_mobs.enchanted_book.locked",
                        eyesNeeded).withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return;
        }
    }
}
