package fr.shoqapik.btemobs.menu.container.explorer_menu;

import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class TableExplorerMenu extends AbstractContainerMenu {

    public final ExplorerTableBlockEntity craftSlots;

    protected final Level level;
    public int id;
    public ContainerData data;
    public TableExplorerMenu(int i, Inventory inventory, FriendlyByteBuf buf) {
        this(i,inventory,new ExplorerTableBlockEntity(),new SimpleContainerData(1));
    }
    public TableExplorerMenu(int p_39356_, Inventory p_39357_, ExplorerTableBlockEntity container, ContainerData data) {
        super(BteMobsContainers.EXPLORER_TABLE_MENU.get(), p_39356_);
        this.data=data;
        this.id=data.get(0);
        this.craftSlots= container;
        this.addSlot(new CraftInputSlot(container, 0, 233,9));
        this.addSlot(new CraftInputSlot(container, 1, 254,9));
        this.addSlot(new CraftInputSlot(container, 2, 212,29));
        this.addSlot(new CraftInputSlot(container, 3, 233,29));
        this.addSlot(new CraftInputSlot(container, 4, 254,29));
        this.addSlot(new CraftInputSlot(container, 5, 233,50));

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
    }



    public int getEntityId(){
        return this.data.get(0);
    }

    protected boolean isIngredient(ItemStack p_38978_) {
        return this.level.getRecipeManager().getRecipeFor(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get(), new ExplorerTableBlockEntity(p_38978_), this.level).isPresent();
    }

    public boolean recipeMatches(Recipe<? super ExplorerTableBlockEntity> p_39384_) {
        return p_39384_.matches(this.craftSlots,this.level);
    }


    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38987_) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_38987_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (p_38987_>=0 && p_38987_<6) {
                if (this.isIngredient(itemstack)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }else if (p_38987_ >= 5 && p_38987_ < 32) {
                if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (p_38987_ >= 31 && p_38987_ < 41 && !this.moveItemStackTo(itemstack1, 5, 32, false)) {
                return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
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


    public ItemStack assemble(ExplorerRecipe p_39384_){
        return p_39384_.assemble(this.craftSlots);
    }


    public void placeRecipe(Player player,ItemStack item) {
        Optional<ExplorerRecipe> optionalRecipe = player.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get()).stream().filter(e->e.getResultItem().getItem()==item.getItem()).findFirst();

        for(int i = 0; i < 6; i++){
            Slot slot = getSlot(i);
            if(slot.hasItem()){
                player.getInventory().add(slot.getItem());
            }
        }
        if(optionalRecipe.isPresent()){
            ExplorerRecipe recipe=optionalRecipe.get();
            if(recipe.hasItems(player)){
                int index = 0;
                for(ItemStack requieredItem: recipe.getRequiredItems().getItems()){
                    int removed = 0;
                    for(ItemStack stack : player.getInventory().items){
                        if(stack.getItem() == requieredItem.getItem()){
                            if(removed < requieredItem.getCount()) {
                                int toRemove = requieredItem.getCount() - removed;
                                if (toRemove > stack.getCount()) toRemove = stack.getCount();
                                stack.shrink(toRemove);
                                removed += toRemove;
                            }
                        }
                    }
                    this.craftSlots.setItem(index,requieredItem.copy());
                    this.slotsChanged(this.craftSlots);
                    index ++;
                }
            }
        }
    }


}
