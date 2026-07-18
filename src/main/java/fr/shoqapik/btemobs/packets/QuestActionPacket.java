package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.UnlockRecipeRewardData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
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
                                        ItemStack reward = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.itemId)),data.count);
                                        ctx.get().getSender().getInventory().add(reward);
                                    }else if (data.type == RewardData.Type.UNLOCK_RECIPE){
                                        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.itemId)));
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
                                                                    BteMobsMod.LOGGER.info("Se agrego el recipe :{}",data.itemId);
                                                                },
                                                                () -> BteMobsMod.LOGGER.info("Recipe not found: {}", data.itemId)
                                                        );
                                            }
                                        }else {
                                            BteMobsMod.LOGGER.info("No se puede convertir");
                                        }


                                    }
                                }
                                cap.completeQuest(msg.quest.id);
                                cap.completeQuest(msg.quest);
                            }
                        }
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
        Quest quest = Quest.decode(packetBuffer);
        int action = packetBuffer.readInt();
        int idPlayer = packetBuffer.readInt();
        return new QuestActionPacket(quest,action,idPlayer);
    }

    public static void encode(QuestActionPacket msg, FriendlyByteBuf packetBuffer) {
        BteMobsMod.LOGGER.info("encode");
        Quest.encode(msg.quest,packetBuffer);
        packetBuffer.writeInt(msg.action);
        packetBuffer.writeInt(msg.idPlayer);
    }
}
