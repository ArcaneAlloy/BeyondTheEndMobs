package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.menu.container.BlacksmithCraftContainer;
import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.packets.ActionPacket;
import fr.shoqapik.btemobs.packets.CraftItemPacket;
import fr.shoqapik.btemobs.packets.ToggleCraftButton;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.resource.GeckoLibCache;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.ConstantValue;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.keyframe.AnimationPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlacksmithCraftMenu extends RecipeBookMenu<BlacksmithCraftContainer> {

    private final Player player;
    private final BlacksmithCraftContainer craftSlots = new BlacksmithCraftContainer(this, 3, 3);
    public Recipe<?> selectedRecipe = null;
    private final int entityId;

    public BlacksmithCraftMenu(int id, Inventory inventory, int entityId) {
        super(BteMobsContainers.BLACKSMITH_CRAFT_MENU.get(), id);
        this.player = inventory.player;
        this.entityId = entityId;

		// X: 62 PRIMEIRA | 80 SEGUNDA | 98 TERCEIRA
		// Y: 20 PRIMEIRA | 38 SEGUNDA
        this.addSlot(new CraftInputSlot(this.craftSlots, 0, 62, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 1, 80, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 2, 98, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 3, 62, 38));
        this.addSlot(new CraftInputSlot(this.craftSlots, 4, 80, 38));
        this.addSlot(new CraftInputSlot(this.craftSlots, 5, 98, 38));


        for(int k = 0; k < 3; ++k) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }

        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu containerMenu, int slotIndex, ItemStack itemStack) {
                if(!player.level.isClientSide) {
                    BteAbstractEntity entity = (BteAbstractEntity) player.level.getEntity(entityId);
                    if(entity.isWorkBlockEmpty()) {
                        ServerPlayer serverPlayer = (ServerPlayer) player;
                        List<Recipe> list = new ArrayList<>();
                        // Add blacksmith recipes
                        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get(), craftSlots, serverPlayer.getLevel()));
                        // Add blacksmith upgrade recipes
                        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get(), craftSlots, serverPlayer.getLevel()));
                        // Add smithing recipes
                        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(RecipeType.SMITHING, craftSlots, serverPlayer.getLevel()));
                        BteMobsMod.sendToClient(new ToggleCraftButton(!list.isEmpty()), serverPlayer);
                    }
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerMenu, int slotIndex, int itemStack) {

            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.clearContainer(pPlayer, this.craftSlots);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents pItemHelper) {
        this.craftSlots.fillStackedContents(pItemHelper);
    }

    @Override
    public void clearCraftingContent() {
        this.craftSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(Recipe<? super BlacksmithCraftContainer> pRecipe) {
        return pRecipe.matches(this.craftSlots, this.player.level);
    }

    @Override
    public int getResultSlotIndex() {
        return -1;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 6;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.valueOf("BLACKSMITH");
    }

    @Override
    public boolean shouldMoveToInventory(int pSlotIndex) {
        return pSlotIndex != this.getResultSlotIndex();
    }

    @Override
    public void handlePlacement(boolean placeAll, Recipe<?> recipe, ServerPlayer player) {
        for(int i = 0; i < 6; i++) {
            Slot slot = getSlot(i);
            if(slot.hasItem()) {
                player.getInventory().add(slot.getItem());
            }
        }

        if(recipe != null && player.getRecipeBook().contains(recipe)) {
            if(recipe instanceof BteAbstractRecipe) {
                BteAbstractRecipe bteRecipe = (BteAbstractRecipe) recipe;
                if (bteRecipe.hasItems(player)) {
                    int index = 0;
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        ItemStack minecraftItem = null;

                        for (ItemStack itemStack : ingredient.getItems()) {
                            if (player.getInventory().countItem(itemStack.getItem()) >= itemStack.getCount()) {
                                minecraftItem = itemStack;
                                break;
                            }
                        }

                        if (minecraftItem == null) continue; // In theory this is impossible

                        int removed = 0;
                        for (ItemStack stack : player.getInventory().items) {
                            if (stack.getItem() == minecraftItem.getItem()) {
                                if (removed < minecraftItem.getCount()) {
                                    int toRemove = minecraftItem.getCount() - removed;
                                    if (toRemove > stack.getCount()) toRemove = stack.getCount();
                                    if(minecraftItem.getCount() <= stack.getCount()) {
                                        int count = minecraftItem.getCount();
                                        minecraftItem = stack.copy();
                                        minecraftItem.setCount(count);
                                    }
                                    stack.shrink(toRemove);
                                    removed += toRemove;
                                }
                            }
                        }

                        this.craftSlots.setItem(index, minecraftItem.copy());
                        //this.getSlot(index).set(minecraftItem.copy());
                        index += 1;
                    }
                } else {
                    player.connection.send(new ClientboundPlaceGhostRecipePacket(this.containerId, recipe));
                }
            } else if(recipe instanceof UpgradeRecipe) {
                UpgradeRecipe upgradeRecipe = (UpgradeRecipe) recipe;
                ItemStack base = null;
                ItemStack addition = null;

                for (ItemStack stack : player.getInventory().items) {
                    if(upgradeRecipe.base.test(stack)) base = stack;
                    if(upgradeRecipe.isAdditionIngredient(stack)) addition = stack;
                    if(base != null && addition != null) break;
                }

                if(base != null && addition != null) {
                    ItemStack baseStack = base.copy();
                    baseStack.setCount(1);
                    ItemStack additionStack = addition.copy();
                    additionStack.setCount(1);
                    this.craftSlots.setItem(0, baseStack);
                    this.craftSlots.setItem(1, additionStack);

                    base.shrink(1);
                    addition.shrink(1);
                } else {
                    player.connection.send(new ClientboundPlaceGhostRecipePacket(this.containerId, recipe));
                }
            } else {
                super.handlePlacement(placeAll, recipe, player);
            }
        }
    }

    public void craftItemClient(Recipe<?> recipe) {
        //if(selectedRecipe == null) return;
        BteMobsMod.sendToServer(new CraftItemPacket(recipe));
        selectedRecipe = null;
    }

    public void craftItemServer(ServerPlayer serverPlayer, Optional<? extends Recipe<?>> clickedRecipe) {
        List<Recipe> list = new ArrayList<>();
        // Add blacksmith recipes
        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get(), craftSlots, serverPlayer.getLevel()));
        // Add blacksmith upgrade recipes
        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get(), craftSlots, serverPlayer.getLevel()));
        // Add smithing recipes
        list.addAll(serverPlayer.getServer().getRecipeManager().getRecipesFor(RecipeType.SMITHING, craftSlots, serverPlayer.getLevel()));

        Recipe recipe = null;
        if(clickedRecipe.isPresent() && list.contains(clickedRecipe.get())) {
            recipe = clickedRecipe.get();
        } else {
            recipe = list.get(0);
        }

        if (recipe != null) {
            for(int i = 0; i < this.craftSlots.getContainerSize(); ++i) {
                Inventory inventory = serverPlayer.getInventory();
                if (inventory.player instanceof ServerPlayer) {
                    this.craftSlots.removeItem(i, this.craftSlots.getItem(i).getCount());

                }
            }

            ItemStack result = recipe.assemble(craftSlots);
            BteAbstractEntity entity = (BteAbstractEntity) player.level.getEntity(entityId);
            if(entity != null) {
                entity.setCraftItem(result);
                entity.setCrafting(true);
                BteMobsMod.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> player.level.getChunkAt(player.getOnPos())), new ActionPacket(entityId, "start_crafting"));
                this.player.closeContainer();
            }
        }
    }
}
