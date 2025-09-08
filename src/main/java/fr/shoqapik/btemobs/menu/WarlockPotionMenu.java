package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.packets.PlaceGhostRecipePacket;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;
import java.util.Optional;

public class WarlockPotionMenu extends AbstractContainerMenu {

    public final SimpleContainer craftSlots;

    protected final Level level;
    public int id;
    public ContainerData data;
    public WarlockPotionMenu(int i, Inventory inventory, FriendlyByteBuf buf) {
        this(i,inventory,new SimpleContainer(7),new SimpleContainerData(1));
    }
    public WarlockPotionMenu(int p_39356_, Inventory p_39357_, SimpleContainer container, ContainerData data) {
        super(BteMobsContainers.POTION_MENU.get(), p_39356_);
        this.data=data;
        this.id=data.get(0);
        this.craftSlots= container;
        this.addSlot(new CraftInputSlot(container, 0, 210, 18){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.REDSTONE) && WarlockPotionMenu.this.craftSlots.getItem(1).isEmpty();
            }
        });
        this.addSlot(new CraftInputSlot(container, 1, 210, 39){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.GLOWSTONE_DUST) && WarlockPotionMenu.this.craftSlots.getItem(0).isEmpty();
            }
        });
        this.addSlot(new CraftInputSlot(container, 2, 233, 23){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.GLASS_BOTTLE);
            }
        });
        this.addSlot(new CraftInputSlot(container, 3, 233,45));
        this.addSlot(new CraftInputSlot(container, 4, (int) 255, (int) (18)){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.GUNPOWDER) && WarlockPotionMenu.this.craftSlots.getItem(5).isEmpty();
            }
        });
        this.addSlot(new CraftInputSlot(container, 5, 255, (38)){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.DRAGON_BREATH) && WarlockPotionMenu.this.craftSlots.getItem(4).isEmpty();
            }
        });

        this.addSlot(new Slot(container, 6, 305, 34) {
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }


            public boolean mayPickup(Player player) {
                if(player.level.isClientSide) return false;
                List<WarlockPotionRecipe> recipes = player.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get());
                if(recipes.isEmpty()) return false;
                return recipes.stream().anyMatch(WarlockPotionMenu.this::recipeMatches);
            }


            public void onTake(Player player, ItemStack itemStack) {
                if(player.level.isClientSide) return;
                WarlockPotionMenu.this.craftSlots.setItem(6,ItemStack.EMPTY);
                ItemStack redstone = WarlockPotionMenu.this.craftSlots.getItem(0);
                ItemStack glowing = WarlockPotionMenu.this.craftSlots.getItem(1);
                ItemStack gunpowder = WarlockPotionMenu.this.craftSlots.getItem(4);
                ItemStack dragonBreath = WarlockPotionMenu.this.craftSlots.getItem(5);
                ItemStack bottle = WarlockPotionMenu.this.craftSlots.getItem(2);
                ItemStack ingredient = WarlockPotionMenu.this.craftSlots.getItem(3);
                if(!redstone.isEmpty()){
                    redstone.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(0,redstone);
                }else if (!glowing.isEmpty()){
                    glowing.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(1,glowing);
                }
                if (!gunpowder.isEmpty()){
                    gunpowder.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(4,gunpowder);
                }else if (!dragonBreath.isEmpty()){
                    dragonBreath.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(5,dragonBreath);
                }
                if(bottle.getCount()<=1){
                    WarlockPotionMenu.this.craftSlots.setItem(2,ItemStack.EMPTY);
                }else {
                    bottle.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(2,bottle);
                }
                if(ingredient.getCount()<=1){
                    WarlockPotionMenu.this.craftSlots.setItem(3,ItemStack.EMPTY);
                }else {
                    ingredient.shrink(1);
                    WarlockPotionMenu.this.craftSlots.setItem(3,ingredient);
                }
            }
        });
        this.level = p_39357_.player.level;
        for(int k = 0; k < 3; ++k) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(p_39357_, i1 + k * 9 + 9, 161 + i1 * 18, 84 + k * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(p_39357_, l, 161 + l * 18, 142));
        }
        this.addDataSlots(data);
        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int i, ItemStack itemStack) {
                if(level.isClientSide)return;
                Optional<WarlockPotionRecipe> optionalRecipe = level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get()).stream().filter(e->e.getIngredientPrimary().getItem()==menu.getSlot(3).getItem().getItem()).findFirst();

                if(!optionalRecipe.isPresent()) {
                    WarlockPotionMenu.this.craftSlots.setItem(6,ItemStack.EMPTY);
                    return;
                }
                SimpleContainer container1 = WarlockPotionMenu.this.craftSlots;
                if(container1.getItem(2).is(Items.GLASS_BOTTLE)){
                    ItemStack result = assemble(optionalRecipe.get());
                    container1.setItem(6, result);
                }else {
                    container1.setItem(6,ItemStack.EMPTY);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int i1) {}
        });
    }


    public int getEntityId(){
        return this.data.get(0);
    }

    protected boolean isIngredient(ItemStack p_38978_) {
        return this.level.getRecipeManager().getRecipeFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get(),  new SimpleContainer(p_38978_), this.level).isPresent();
    }

    public boolean recipeMatches(Recipe<SimpleContainer> p_39384_) {
        return p_39384_.matches(this.craftSlots,this.level);
    }

    protected void slotChangedCraftingGrid(Level pLevel, SimpleContainer pContainer) {
        if (!pLevel.isClientSide) {
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<WarlockPotionRecipe> optional = pLevel.getServer().getRecipeManager().getRecipeFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get(), pContainer, pLevel);
            if (optional.isPresent()) {
                WarlockPotionRecipe craftingrecipe = optional.get();
                if (craftingrecipe.matches(pContainer,pLevel)){
                    itemstack = craftingrecipe.assemble(pContainer);
                }
            }
            pContainer.setItem(6, itemstack);
            this.broadcastChanges();
        }
    }


    /**
     * Callback for when the crafting matrix is changed.
     */
    public void slotsChanged(Container pInventory) {
        slotChangedCraftingGrid(this.level, this.craftSlots);
    }
    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38987_) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_38987_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (p_38987_>=0 && p_38987_<7) {
                if (this.isIngredient(itemstack)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }else if (p_38987_ >= 6 && p_38987_ < 33) {
                if (!this.moveItemStackTo(itemstack1, 33, 42, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (p_38987_ >= 32 && p_38987_ < 42 && !this.moveItemStackTo(itemstack1, 6, 33, false)) {
                return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(itemstack1, 6, 42, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(p_38941_, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }



    public ItemStack assemble(WarlockPotionRecipe p_39384_){
        return p_39384_.assemble(this.craftSlots);
    }


    public void placeRecipe(ServerPlayer player, ItemStack item) {
        Optional<WarlockPotionRecipe> optionalRecipe = player.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get()).stream().filter(e->{
            return e.getIngredientPrimary().getItem()==item.getItem();
        }).findFirst();
        ItemStack stack1 = craftSlots.getItem(3);
        ItemStack bottle = craftSlots.getItem(2);
        if(!stack1.isEmpty()){
            player.getInventory().add(stack1.copy());
            craftSlots.setItem(3,ItemStack.EMPTY);
        }
        if(!bottle.isEmpty()){
            player.getInventory().add(bottle.copy());
            craftSlots.setItem(2,bottle.copy());
        }

        if(optionalRecipe.isPresent()){
            WarlockPotionRecipe recipe=optionalRecipe.get();
            if(recipe.hasItems(player,craftSlots)){
                Inventory inventory = player.getInventory();
                boolean hasIngredientPrimary = false;
                boolean hasBottle = false;
                for (int index=0 ; index<inventory.getContainerSize() ; index++){
                    ItemStack stack = inventory.getItem(index);
                    if(!hasIngredientPrimary && stack.is(recipe.getIngredientPrimary().getItem())){
                        craftSlots.setItem(3,stack.copy());
                        inventory.removeItem(stack);
                        hasIngredientPrimary = true;
                    }
                    if(!hasBottle && stack.is(Items.GLASS_BOTTLE)){
                        craftSlots.setItem(2,stack.copy());
                        inventory.removeItem(stack);
                        hasBottle = true;
                    }
                    if(hasIngredientPrimary && hasBottle){
                        break;
                    }
                }
            }else {
                BteMobsMod.sendToClient(new PlaceGhostRecipePacket(this.containerId,recipe),player);
            }
        }
    }


}
