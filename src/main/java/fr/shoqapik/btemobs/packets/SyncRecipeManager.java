package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.StatRewardData;
import fr.shoqapik.btemobs.capability.UnlockAction;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

public class SyncRecipeManager {

    private final int entityId;
    private CompoundTag data;
    private boolean wasDeath;
    public SyncRecipeManager(int entityId, CompoundTag nbt,boolean wasDeath) {
        this.entityId = entityId;
        this.data = nbt;
        this.wasDeath = wasDeath;
    }

    public static void handle(SyncRecipeManager msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
//                BteMobsMod.LOGGER.debug("Handler : {}",msg.data);
                msg.handleClient();
            }
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(this.entityId);

        if (entity instanceof Player player) {
            RecipeCapability cap = RecipeCapability.get(player);
            if(cap!=null){
                Map<RecipeType<?>,List<Recipe<?>>> map = new HashMap<>();
                Map<Quest,List<StatRewardData>> map1 = new HashMap<>();
                Map<BteNpcType,Map<Quest,QuestStateData>> map2 = new HashMap<>();
                if(!wasDeath){
                    if(data.contains("manager")){
                        ListTag list = data.getList("manager",10);
                        for (int i = 0 ; i < list.size() ; i ++){
                            CompoundTag tag = list.getCompound(i);
                            if (!ResourceLocation.isValidResourceLocation(tag.getString("type")))continue;

                            RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(tag.getString("type")));
                            if(type==null)continue;
                            List<Recipe<?>> recipes = new ArrayList<>();
                            if(tag.contains("recipes")){
                                ListTag list1 = tag.getList("recipes",10);
                                for (int j = 0 ; j < list1.size() ; j++){
                                    CompoundTag tag1 = list1.getCompound(j);
                                    Optional<? extends Recipe<?>> recipe = Minecraft.getInstance().level.getRecipeManager().byKey(new ResourceLocation(tag1.getString("recipe")));
                                    recipe.ifPresent(recipes::add);
                                }
                            }
                            map.put(type,recipes);
                        }

                        cap.setRecipeManager(map);

                    }
                    for (BteNpcType type : BteNpcType.values()){
                        map2.put(type,new HashMap<>());
                    }
                    if (data.contains("quests")){
                        ListTag listTag = data.getList("quests",10);

                        for (int i = 0 ; i < listTag.size() ; i++){
                            CompoundTag data1 = listTag.getCompound(i);
                            BteNpcType type = BteNpcType.valueOf(data1.getString("type"));
                            if (data1.contains("list")){
                                ListTag listTag1 = data1.getList("list",10);
                                for (int j = 0;j < listTag1.size() ; j++){
                                    CompoundTag data2 = listTag1.getCompound(j);
                                    Quest quest = QuestManager.getQuest(data2.getString("id"));
                                    map2.get(type).put(quest,new QuestStateData(data2.getCompound("data")));
                                }
                            }
                        }
                    }
                    List<UnlockAction> unlockActions = new ArrayList<>();
                    if (data.contains("unlockAction")){
                        ListTag list = data.getList("unlockAction",10);
                        for (int i = 0 ; i < list.size() ; i++){
                            CompoundTag data1 = list.getCompound(i);
                            unlockActions.add(new UnlockAction(data1));
                        }
                    }
                    cap.quests = map2;
                    cap.unlockActions = unlockActions;
                }else {
                    cap.deserializeNBT(data);
                }

            }
        }
    }

    public static void encode(SyncRecipeManager msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeNbt(msg.data);
        buf.writeBoolean(msg.wasDeath);
    }

    public static SyncRecipeManager decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        CompoundTag data = buf.readNbt();
        boolean wasDeath = buf.readBoolean();
        return new SyncRecipeManager(entityId, data,wasDeath);
    }
}
