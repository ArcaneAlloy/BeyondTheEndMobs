package fr.shoqapik.btemobs.packets;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.ServerData;
import fr.shoqapik.btemobs.RecipeSharing;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.quest.ItemRewardData;
import fr.shoqapik.btemobs.quest.QuestManager;
import fr.shoqapik.btemobs.quest.QuestRecipeLocks;
import fr.shoqapik.btemobs.quest.QuestTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null || msg.quest == null || player.getId() != msg.idPlayer) return;
                    RecipeCapability cap = RecipeCapability.get(player);
                    if (cap == null) return;
                    claim(player, cap, msg.quest.id);
                }
                case 2 ->{
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null || msg.quest == null || player.getId() != msg.idPlayer) return;
                    RecipeCapability cap = RecipeCapability.get(player);
                    if (cap == null) return;
                    reclaimRewards(player, cap, msg.quest.id);
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

    /**
     * Reclamar una quest (modo cooperativo).
     *  - Se usa la definición del servidor (no la que manda el cliente) y se comprueba que esté completa.
     *  - Las quests son compartidas: una vez reclamada, nadie más puede reclamarla.
     *  - Los objetos de recompensa son solo para quien reclama; las recetas se desbloquean para todos.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void claim(ServerPlayer player, RecipeCapability cap, ResourceLocation questId) {
        Quest quest = QuestManager.getQuest(questId.toString());
        QuestStateData state = cap.findState(questId);
        if (quest == null || state == null) return;
        if (state.isReclaim) {
            player.displayClientMessage(Component.translatable("gui.bte_mobs.quest.already_claimed").withStyle(ChatFormatting.YELLOW), true);
            cap.dirty = true;
            return;
        }
        // Recalcula con el inventario de quien reclama (las tareas COLLECT dependen de él)
        cap.checkChangedInventory();
        if (!state.isComplete) {
            player.displayClientMessage(Component.translatable("gui.bte_mobs.quest.claim_missing").withStyle(ChatFormatting.RED), true);
            cap.dirty = true;
            return;
        }

        // Primero se retiran los objetos entregados
        for (TaskData data : quest.getTasks()){
            if (data.type == TaskData.Type.COLLECT){
                String spec = data.getEntityIdLocation();
                player.getInventory().clearOrCountMatchingItems(
                        stack -> QuestTriggers.matchesItem(spec, stack),
                        data.count,
                        player.inventoryMenu.getCraftSlots()
                );
            }
        }

        for (RewardData data : quest.getRewards()){
            if (data.type == RewardData.Type.ITEM){
                ItemStack reward = data instanceof ItemRewardData itemData
                        ? itemData.createStack()
                        : new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getObjectId())),data.count);
                if (!player.getInventory().add(reward)) player.drop(reward, false);
            }else if (data.type == RewardData.Type.UNLOCK_RECIPE || data.type == RewardData.Type.UNLOCK_RECIPES_BY_INGREDIENT){
                // Receta que además necesita otra quest reclamada: si aún no lo está, se desbloquea al reclamar esa otra
                if (data instanceof UnlockRecipeRewardData d && d.requiresQuest != null && !isClaimed(cap, d.requiresQuest)) {
                    BteMobsMod.LOGGER.info("Quest {}: recetas {} pendientes de la quest {}", quest.id, d.recipeId, d.requiresQuest);
                    continue;
                }
                // Todas las variantes de la receta, para todos los jugadores
                List<Recipe<?>> recipes = QuestRecipeLocks.recipesFor(
                        player.getServer().getRecipeManager(), QuestManager.getQuests(), data);
                RecipeSharing.unlockForEveryone(recipes);
                BteMobsMod.LOGGER.info("Quest {}: desbloqueadas {} recetas ({})", quest.id, recipes.size(), data.getObjectId());
            }else if (data.type == RewardData.Type.UNLOCK_ZONE){
                // Acepta "the_twilight_forest" y "bte_mobs:the_twilight_forest"
                String zonePath = data.getObjectId().contains(":") ? data.getObjectId().substring(data.getObjectId().indexOf(':') + 1) : data.getObjectId();
                if (Objects.equals(zonePath, "the_twilight_forest")) {
                    fr.shoqapik.btemobs.quest.TwilightPortalUnlock.open(player.getServer());
                }
            }
        }

        state.isComplete = true;
        cap.completeQuest(quest.id);
        cap.completeQuest(quest);
        unlockDeferredRecipes(player, cap, quest.id);
        cap.checkChangedInventory();

        // Aviso a todo el lobby
        Component msg = Component.translatable("gui.bte_mobs.quest.claimed_by",
                player.getDisplayName(), Component.translatable("title.quest." + quest.id.getPath()))
                .withStyle(ChatFormatting.GOLD);
        player.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    /**
     * Volver a pedir las recompensas de objeto de una quest ya reclamada (quests con "reclaim" en el JSON, p. ej. los
     * mapas de Antonio), pagando el coste (por defecto 1 Skeleton Skull). No repite recetas, zonas ni experiencia.
     */
    private static void reclaimRewards(ServerPlayer player, RecipeCapability cap, ResourceLocation questId) {
        Quest quest = QuestManager.getQuest(questId.toString());
        QuestStateData state = cap.findState(questId);
        if (quest == null || state == null || quest.getReclaim() == null || !state.isReclaim) return;
        Quest.ReclaimCost cost = quest.getReclaim();
        Item costItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(cost.itemId));
        if (costItem == null || costItem == net.minecraft.world.item.Items.AIR) return;

        int have = player.getInventory().countItem(costItem);
        if (have < cost.count) {
            player.displayClientMessage(Component.translatable("gui.bte_mobs.quest.reclaim_missing",
                    cost.count, new ItemStack(costItem).getHoverName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(costItem), cost.count, player.inventoryMenu.getCraftSlots());

        for (RewardData data : quest.getRewards()) {
            if (data.type != RewardData.Type.ITEM) continue;
            ItemStack reward = data instanceof ItemRewardData itemData
                    ? itemData.createStack()
                    : new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getObjectId())), data.count);
            if (!player.getInventory().add(reward)) player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable("gui.bte_mobs.quest.reclaimed",
                Component.translatable("title.quest." + quest.id.getPath())).withStyle(ChatFormatting.GREEN), true);
        BteMobsMod.LOGGER.info("Quest {}: recompensas pedidas de nuevo por {}", quest.id, player.getGameProfile().getName());
    }

    /** true si la quest (id "mod:quest" o solo "quest", namespace bte_mobs) ya está reclamada. */
    private static boolean isClaimed(RecipeCapability cap, String questId) {
        ResourceLocation id = ResourceLocation.tryParse(questId.contains(":") ? questId : "bte_mobs:" + questId);
        if (id == null) return false;
        QuestStateData s = cap.findState(id);
        return s != null && s.isReclaim;
    }

    /**
     * Recetas con "requiresQuest" de quests ya reclamadas que esperaban a la quest que se acaba de reclamar.
     * (Las de la propia quest ya se han desbloqueado en el bucle de recompensas si su requisito estaba cumplido.)
     */
    private static void unlockDeferredRecipes(ServerPlayer player, RecipeCapability cap, ResourceLocation claimedId) {
        for (Quest other : QuestManager.getQuests()) {
            if (other.id.equals(claimedId) || !isClaimed(cap, other.id.toString())) continue;
            for (RewardData data : other.getRewards()) {
                if (!(data instanceof UnlockRecipeRewardData d) || d.requiresQuest == null) continue;
                ResourceLocation req = ResourceLocation.tryParse(d.requiresQuest.contains(":") ? d.requiresQuest : "bte_mobs:" + d.requiresQuest);
                if (!claimedId.equals(req)) continue;
                List<Recipe<?>> recipes = QuestRecipeLocks.recipesFor(
                        player.getServer().getRecipeManager(), QuestManager.getQuests(), data);
                RecipeSharing.unlockForEveryone(recipes);
                BteMobsMod.LOGGER.info("Quest {}: desbloqueadas {} recetas pendientes de {} ({})", other.id, recipes.size(), claimedId, d.recipeId);
            }
        }
    }

    private static RecipeType<?> getRecipeTypeForId(UnlockRecipeRewardData data) {
        for (RecipeType<?> type1 : ForgeRegistries.RECIPE_TYPES.getValues()){
            if (type1.toString().equals(data.recipeType)){
                return type1;
            }
        }
        return null;
    }


    public static QuestActionPacket decode(FriendlyByteBuf buf) {
        Quest quest = null;

        if (buf.readBoolean()) {
            quest = Quest.decode(buf);
        }

        int action = buf.readInt();
        int idPlayer = buf.readInt();

        return new QuestActionPacket(quest, action, idPlayer);
    }

    public static void encode(QuestActionPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.quest != null);

        if (msg.quest != null) {
            Quest.encode(msg.quest, buf);
        }

        buf.writeInt(msg.action);
        buf.writeInt(msg.idPlayer);
    }
}
