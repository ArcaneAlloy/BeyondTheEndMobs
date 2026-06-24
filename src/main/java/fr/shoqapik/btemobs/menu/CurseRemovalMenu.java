package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.registry.BteMobsContainers;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CurseRemovalMenu extends AbstractContainerMenu {

    protected final ResultContainer resultSlots = new ResultContainer();

    public final Container inputSlots = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            CurseRemovalMenu.this.slotsChanged(this);
        }
    };



    public final Player player;
    public final Level level;

    public List<Enchantment> curses = new ArrayList<>();

    public CurseRemovalMenu(int id, Inventory inventory) {
        super(BteMobsContainers.WARLOCK_REMOVE_CURSE_MENU.get(), id);

        this.player = inventory.player;
        this.level = inventory.player.level;

        this.addSlot(new Slot(inputSlots, 0, 203 - 90, 33));

        this.addSlot(new Slot(resultSlots, 0, 308 - 90, 33) {

            @Override
            public boolean mayPickup(Player player) {
                return canRemoveCurse();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);

                inputSlots.getItem(0).shrink(1);

                consumeSkulls(player);

                player.experienceLevel -= 20;
            }
        });

        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
                if(menu != CurseRemovalMenu.this)
                    return;

                updateCurses();

                if(canRemoveCurse()) {
                    getSlot(1).set(removeSelectedCurse());
                } else {
                    getSlot(1).set(ItemStack.EMPTY);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu menu, int id, int value) {}
        });

        for(int row = 0; row < 3; ++row) {
            for(int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        inventory,
                        col + row * 9 + 9,
                        -90 + 184 + col * 18,
                        84 + row * 18
                ));
            }
        }

        for(int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    inventory,
                    col,
                    -90 + 184 + col * 18,
                    142
            ));
        }
    }

    private void updateCurses() {

    }

    public ItemStack removeSelectedCurse() {
        updateCurses();

        ItemStack copy = inputSlots.getItem(0).copy();
        List<Enchantment> curses=new ArrayList<>();
        Map<Enchantment,Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(copy));
        if (!EnchantedBookItem.getEnchantments(copy).isEmpty()){
            ListTag tag = EnchantedBookItem.getEnchantments(copy);
            for (int i = 0 ; i < tag.size() ; i++ ){
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(EnchantmentHelper.getEnchantmentId(tag.getCompound(i)));
                if (enchantment!=null && enchantment.isCurse()){
                    curses.add(enchantment);
                }
            }
        }else {
            curses = enchants.keySet().stream().filter(Enchantment::isCurse).toList();
        }



        for (Enchantment curse : curses){
            enchants.remove(curse);
        }

        EnchantmentHelper.setEnchantments(enchants, copy);

        return copy;
    }

    public boolean canRemoveCurse() {
        updateCurses();

        if(player.experienceLevel < 20)
            return false;

        int skulls = 0;

        for(ItemStack stack : player.getInventory().items) {

            if(stack.is(Items.SKELETON_SKULL)) {
                skulls += stack.getCount();
            }

        }

        return skulls >= 5;
    }

    private void consumeSkulls(Player player) {

        int remaining = 5;

        for(ItemStack stack : player.getInventory().items) {
            if(!stack.is(Items.SKELETON_SKULL))
                continue;

            int remove = Math.min(remaining, stack.getCount());

            stack.shrink(remove);

            remaining -= remove;

            if(remaining <= 0)
                return;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        updateCurses();
        if(id >= 0) {
            if(canRemoveCurse()) {
                getSlot(1).set(removeSelectedCurse());
            }
            broadcastChanges();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inputSlots.clearContent();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}