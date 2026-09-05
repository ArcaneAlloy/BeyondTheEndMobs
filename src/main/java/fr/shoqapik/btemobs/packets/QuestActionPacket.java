package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.ServerData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.TaskData;
import fr.shoqapik.btemobs.quest.UnlockRecipeRewardData;
import mc.duzo.ender_journey.EndersJourney;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import twilightforest.init.TFBlocks;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class QuestActionPacket {
    public int idPlayer;
    public final Quest quest;
    public final int action;
    public QuestActionPacket(Quest quest,int action,int idPlayer){
        this.quest = quest;
        this.action = action;
        this.idPlayer = idPlayer;
    }

    public static void handle(QuestActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->{
            switch (msg.action){
                case 0 ->{
                    RecipeCapability cap = RecipeCapability.get(ctx.get().getSender());
                    if (cap!=null){
                        if (ctx.get().getSender().getId() == msg.idPlayer){
                            if (ctx.get().getSender()!=null){
                                for (RewardData data : msg.quest.getRewards()){
                                    if (data.type == RewardData.Type.ITEM){
                                        ItemStack reward = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getObjectId())),data.count);
                                        ctx.get().getSender().getInventory().add(reward);
                                    }else if (data.type == RewardData.Type.UNLOCK_RECIPE){
                                        if (data.getObjectId().contains("#")){
                                            ResourceLocation tagId = new ResourceLocation(data.getObjectId().substring(1));

                                            TagKey<Item> tag = ItemTags.create(tagId);

                                            ServerPlayer player = ctx.get().getSender();
                                            ServerLevel level = player.getServer().overworld();

                                            IForgeRegistry<Item> itemRegistry = ForgeRegistries.ITEMS;
                                            for (ResourceLocation rl : itemRegistry.getKeys()){
                                                ItemStack item = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
                                                for (TagKey<Item> tagItem : item.getTags().toList()){
                                                    BteMobsMod.LOGGER.info("item {}",tagItem.toString());
                                                }

                                                if (item.is(tag)){
                                                    BteMobsMod.LOGGER.info("tiene el tag correcto {}",item);
                                                    if (data instanceof UnlockRecipeRewardData unlockData) {
                                                        RecipeType<?> type = getRecipeTypeForId(unlockData);
                                                        BteMobsMod.LOGGER.info("type {}",type);

                                                        if (type == null) {
                                                            return;
                                                        }

                                                        player.getServer().getRecipeManager().getRecipes().stream().filter(recipe -> recipe.getType().equals(type))
                                                                .filter(recipe -> recipe.getResultItem().getItem().equals(item.getItem()))
                                                                .findFirst()
                                                                .ifPresentOrElse(
                                                                        recipe -> {
                                                                            cap.addRecipeForType(type, recipe);
                                                                            BteMobsMod.LOGGER.info("Se agrego recipe {} del tag {}", item, data.getObjectId());
                                                                        },
                                                                        () -> BteMobsMod.LOGGER.info(
                                                                                "Recipe not found for item {} from tag {}", item, data.getObjectId()));
                                                    }
                                                }
                                            }
                                        }else{
                                            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getObjectId())));
                                            if (data instanceof  UnlockRecipeRewardData){
                                                RecipeType<?> type = getRecipeTypeForId((UnlockRecipeRewardData) data);
                                                if (type != null){
                                                    ctx.get().getSender().server.getRecipeManager()
                                                            .getRecipes()
                                                            .stream()
                                                            .filter(recipe -> recipe.getType().toString().equals(type.toString()))
                                                            .filter(recipe -> stack.getItem().equals(recipe.getResultItem().getItem()))
                                                            .findFirst().ifPresentOrElse(
                                                                    recipe -> {
                                                                        cap.addRecipeForType(type, recipe);
                                                                        BteMobsMod.LOGGER.info("Se agrego el recipe :{}",data.getObjectId());
                                                                    },
                                                                    () -> BteMobsMod.LOGGER.info("Recipe not found: {}", data.getObjectId())
                                                            );
                                                }
                                            }
                                        }

                                    }else if (data.type == RewardData.Type.UNLOCK_ZONE){
                                        ServerLevel level = (ServerLevel) ctx.get().getSender().level;

                                        if (Objects.equals(data.getObjectId(), "the_twilight_forest") && !ServerData.get().unlockTwilightForestPortal()){
                                            BlockPos pos1 = new BlockPos(-1,78,1);
                                            BlockPos pos2 = new BlockPos(1,78,-1);
//
                                            for (BlockPos pos : BlockPos.betweenClosed(pos1,pos2)){
                                                BlockPos blockPos = new BlockPos(pos.getX(),pos.getY(),pos.getZ());
                                                level.setBlock(blockPos, TFBlocks.TWILIGHT_PORTAL.get().defaultBlockState(),2);
                                            }
                                            ServerData.get().unlockPortal();
                                        }
                                    }
                                }
                                Player player = ctx.get().getSender();

                                for (TaskData data : msg.quest.getTasks()){
                                    if (data.type == TaskData.Type.COLLECT){
                                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getEntityIdLocation()));
                                        player.getInventory().clearOrCountMatchingItems(
                                                stack -> stack.is(item),
                                                10,
                                                player.inventoryMenu.getCraftSlots()
                                        );
                                    }
                                }
                                cap.completeQuest(msg.quest.id);
                                cap.completeQuest(msg.quest);
                            }
                        }
                    }
                }
                case 1->{
                    RecipeCapability cap = RecipeCapability.get(ctx.get().getSender());
                    if (cap!=null){
                        cap.checkChangedInventory();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static RecipeType<?> getRecipeTypeForId(UnlockRecipeRewardData data) {
        for (RecipeType<?> type1 : ForgeRegistries.RECIPE_TYPES.getValues()){
            if (type1.toString().equals(data.recipeType)){
                return type1;
            }
        }
        return null;
    }


    public static QuestActionPacket decode(FriendlyByteBuf packetBuffer) {
        Quest quest;
        if (packetBuffer.array().length>2){
             quest= Quest.decode(packetBuffer);
        }else {
            quest = null;
        }
        int action = packetBuffer.readInt();
        int idPlayer = packetBuffer.readInt();
        return new QuestActionPacket(quest,action,idPlayer);
    }

    public static void encode(QuestActionPacket msg, FriendlyByteBuf packetBuffer) {
        if (msg.quest!=null){
            Quest.encode(msg.quest,packetBuffer);
        }
        packetBuffer.writeInt(msg.action);
        packetBuffer.writeInt(msg.idPlayer);
    }
}
