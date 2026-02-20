package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.RecipeCapability;
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
                BteMobsMod.LOGGER.debug("Handler : {}",msg.data);
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
                if(!wasDeath){
                    if(data.contains("manager")){
                        ListTag list = data.getList("manager",10);
                        for (int i = 0 ; i < list.size() ; i ++){
                            CompoundTag tag = list.getCompound(i);
                            BteMobsMod.LOGGER.debug("tag :{}",tag);
                            if (!ResourceLocation.isValidResourceLocation(tag.getString("type")))continue;
                            BteMobsMod.LOGGER.debug("type :{}",tag.getString("type"));

                            RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(tag.getString("type")));
                            if(type==null)continue;
                            BteMobsMod.LOGGER.debug("existe el tipo :{}",type);

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
                        BteMobsMod.LOGGER.debug("map :{}",map);

                        BteMobsMod.LOGGER.debug("pre readdata :{}",cap.getRecipeManager());
                        cap.setRecipeManager(map);
                        BteMobsMod.LOGGER.debug("post readdata :{}",cap.getRecipeManager());
                    }
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
