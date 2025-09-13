package fr.shoqapik.btemobs;

import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import com.patataprojects.anvilchanges.repairs.AnvilRepairSet;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.entity.WarlockEntity;
import fr.shoqapik.btemobs.menu.BlacksmithRepairMenu;
import fr.shoqapik.btemobs.menu.BteAbstractCraftMenu;
import fr.shoqapik.btemobs.menu.WarlockCraftMenu;
import fr.shoqapik.btemobs.menu.provider.BlacksmithCraftProvider;
import fr.shoqapik.btemobs.menu.provider.WarlockCraftProvider;
import fr.shoqapik.btemobs.packets.*;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.rumors.Rumor;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.registry.*;
import fr.shoqapik.btemobs.sound.SoundManager;
import mc.duzo.beyondtheend.common.DimensionUtil;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;


@Mod(BteMobsMod.MODID)
public class BteMobsMod {

    public static final String MODID = "bte_mobs";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static double x=0;
    public static double y=0;
    public static double z=0;
    public static double xq=0;
    public static double yq=0;
    public static double zq=0;
    public static int eyes = 0;
    public static Rumor.UnlockLevel unlockLevel = Rumor.UnlockLevel.OVERWORLD;
    public static PageCompendium.UnlockLevel unlockLevel1 = PageCompendium.UnlockLevel.OVERWORLD;
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public BteMobsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        BteMobsBlockEntities.register(bus);
        BteMobsBlocks.register(bus);
        BteMobsContainers.register(bus);
        BteMobsEntities.register(bus);
        BteMobsRecipeSerializers.register(bus);
        BteMobsRecipeTypes.register(bus);
        BteMobsBlocks.ITEMS.register(bus);
        SoundManager.SOUND_EVENTS.register(bus);

        INSTANCE.registerMessage(0, ShowDialogPacket.class, ShowDialogPacket::encode, ShowDialogPacket::decode, ShowDialogPacket::handle);
        INSTANCE.registerMessage(1, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        INSTANCE.registerMessage(2, CheckUnlockRecipePacket.class, CheckUnlockRecipePacket::encode, CheckUnlockRecipePacket::decode, CheckUnlockRecipePacket::handle);
        INSTANCE.registerMessage(3, CraftItemPacket.class, CraftItemPacket::encode, CraftItemPacket::decode, CraftItemPacket::handle);
        INSTANCE.registerMessage(4, ToggleCraftButton.class, ToggleCraftButton::encode, ToggleCraftButton::decode, ToggleCraftButton::handle);
        INSTANCE.registerMessage(5, RenameItemPacket.class, RenameItemPacket::encode, RenameItemPacket::decode, RenameItemPacket::handle);

        INSTANCE.registerMessage(6, StartCraftingItemPacket.class, StartCraftingItemPacket::encode, StartCraftingItemPacket::decode, StartCraftingItemPacket::handle);
        INSTANCE.registerMessage(7, BlockUpdatePacket.class, BlockUpdatePacket::encode, BlockUpdatePacket::decode, BlockUpdatePacket::handle);
        INSTANCE.registerMessage(8, PlaceItemRecipePacket.class, PlaceItemRecipePacket::encode, PlaceItemRecipePacket::decode, PlaceItemRecipePacket::handle);
        INSTANCE.registerMessage(9, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        INSTANCE.registerMessage(10, PartItemPacket.class, PartItemPacket::encode, PartItemPacket::decode, PartItemPacket::handle);
        INSTANCE.registerMessage(11, DirectionPacket.class, DirectionPacket::encode, DirectionPacket::decode, DirectionPacket::handle);
        INSTANCE.registerMessage(12, PlaceGhostRecipePacket.class, PlaceGhostRecipePacket::encode, PlaceGhostRecipePacket::decode, PlaceGhostRecipePacket::handle);
        INSTANCE.registerMessage(13, LastClickedRecipeUpdatePacket.class, LastClickedRecipeUpdatePacket::encode, LastClickedRecipeUpdatePacket::decode, LastClickedRecipeUpdatePacket::handle);
        INSTANCE.registerMessage(14,SyncUnlockLevelPacket.class,SyncUnlockLevelPacket::encode,SyncUnlockLevelPacket::decode,SyncUnlockLevelPacket::handle);
    }

    public static List<EnchantType> getEnchantType (){
        List<EnchantType> types = new ArrayList<>();
        List<WarlockRecipe> recipes = getWarlockRecipe();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues()){
            if(recipes.stream().anyMatch(warlockRecipe -> warlockRecipe.getEnchantment()==enchantment)){
                types.add(new EnchantType(Component.translatable(enchantment.getDescriptionId()),enchantment));
            }
        }
        return types;
    }

    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToAllTracking(MSG message, LivingEntity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }


    public static void handleActionPacket(ActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if(msg.actionType.equals("open_craft")) {
            BteAbstractEntity bteAbstractEntity = (BteAbstractEntity) ctx.get().getSender().getLevel().getEntity(msg.entityId);
            if(bteAbstractEntity == null) return;
            switch (bteAbstractEntity.getNpcType()) {
                case BLACKSMITH -> NetworkHooks.openScreen(ctx.get().getSender(), new BlacksmithCraftProvider(msg.entityId));
                case WARLOCK -> NetworkHooks.openScreen(ctx.get().getSender(), new WarlockCraftProvider(msg.entityId));
                case EXPLORER -> ((ExplorerEntity)bteAbstractEntity).openCraftGui(ctx.get().getSender());
                case DRUID -> ((DruidEntity)bteAbstractEntity).openCraftGui(ctx.get().getSender());
            }
        }

        if(msg.actionType.equals("open_repair")) {
            NetworkHooks.openScreen(ctx.get().getSender(), new SimpleMenuProvider((id, inventory, player) -> {
                Entity entity = ctx.get().getSender().getLevel().getEntity(msg.entityId);
                return new BlacksmithRepairMenu(id, inventory);
            }, Component.literal("Repair")));
        }

        if(msg.actionType.equals("potion")){
            BteAbstractEntity bteAbstractEntity = (BteAbstractEntity) ctx.get().getSender().getLevel().getEntity(msg.entityId);
            if(bteAbstractEntity instanceof WarlockEntity warlockEntity){
                warlockEntity.openPotionGui(ctx.get().getSender());
            }

        }
    }

    public static void handleUnlockRecipePacket(CheckUnlockRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        List<Recipe<?>> recipes = new ArrayList<>();

        List<BteAbstractRecipe> list = new ArrayList<>();
        list.addAll(ServerLifecycleHooks.getCurrentServer().getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get()));
        list.addAll(ServerLifecycleHooks.getCurrentServer().getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get()));
        for(BteAbstractRecipe recipe : list) {
            if(ctx.get().getSender().getRecipeBook().contains(recipe.getId())) continue;
            List<Item> items = new ArrayList<>();
            recipe.getIngredients().stream().map(Ingredient::getItems).forEach(item -> items.addAll(Arrays.stream(item).map(ItemStack::getItem).toList()));

            recipes.add(recipe);
        }

        ctx.get().getSender().awardRecipes(recipes);

        List<Recipe<?>> recipes1 = new ArrayList<>();
        List<WarlockRecipe> recipes2  = getServer().getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());

        for (WarlockRecipe recipe : recipes2){
            if(ServerData.get().isUnlock(recipe)) continue;
            boolean flag = false;
            if(ctx.get().getSender().getInventory().items.stream().anyMatch(e->ServerData.get().getUnlockRecipe(recipe).is(e))){
                ServerData.get().getUnlockRecipe(recipe).setWasFound(true);
            }
            PortalPlayer player = PortalPlayer.get(ctx.get().getSender()).orElse(null);

            if(ServerData.get().getUnlockRecipe(recipe).wasFound && player.getEyesEarn()>=recipe.getNeedEyes()){

                ServerData.get().getUnlockRecipe(recipe).isLock = false;
                flag = true;
            }
            if(flag){
                recipes1.add(recipe);
            }
        }
        ctx.get().getSender().awardRecipes(recipes1);
    }

    public static List<WarlockRecipe> getWarlockRecipe(){
        List<WarlockRecipe> recipes = new ArrayList<>();
        for (UnlockRecipe recipe : ServerData.get().getStructureManager()){
            if(!recipe.isLock && recipe.wasFound){
                recipes.add((WarlockRecipe) recipe.recipe);
            }
        }
        return recipes;
    }

    public static void handleCraftItemPacket(CraftItemPacket msg, Supplier<NetworkEvent.Context> ctx){
        if(ctx.get().getSender().containerMenu instanceof BteAbstractCraftMenu){
            BteAbstractCraftMenu menu = (BteAbstractCraftMenu) ctx.get().getSender().containerMenu;
            Optional<? extends Recipe<?>> recipe = Optional.empty();
            if(msg.recipe != null) recipe = ctx.get().getSender().getServer().getRecipeManager().byKey(msg.recipe);
            menu.craftItemServer(ctx.get().getSender(), recipe);
        }
    }

    public static void handleRenameItemPacket(RenameItemPacket msg, Supplier<NetworkEvent.Context> ctx){
        AbstractContainerMenu $$2 = ctx.get().getSender().containerMenu;
        if ($$2 instanceof BlacksmithRepairMenu repairMenu) {
            if (!repairMenu.stillValid(ctx.get().getSender())) {
                LOGGER.debug("Player {} interacted with invalid menu {}", ctx.get().getSender(), repairMenu);
                return;
            }

            String s = SharedConstants.filterText(msg.getName());
            if (s.length() <= 50) {
                repairMenu.setItemName(s);
            }
        }
    }

    public static void handleLastClickedRecipeUpdatePacket(LastClickedRecipeUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        AbstractContainerMenu menu = ctx.get().getSender().containerMenu;
        if (menu instanceof WarlockCraftMenu warlockMenu) {
            if (!warlockMenu.stillValid(ctx.get().getSender())) {
                LOGGER.debug("Player {} interacted with invalid menu {}", ((NetworkEvent.Context) ctx.get()).getSender(), warlockMenu);
                return;
            }

            Optional<? extends Recipe<?>> recipe = Optional.empty();
            if (msg.recipe != null) recipe = ctx.get().getSender().getServer().getRecipeManager().byKey(msg.recipe);

            warlockMenu.clickedRecipe = recipe;
        }
    }
    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }


}
