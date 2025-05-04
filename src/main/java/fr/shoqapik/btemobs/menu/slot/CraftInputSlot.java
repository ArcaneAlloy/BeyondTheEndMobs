package fr.shoqapik.btemobs.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraftInputSlot extends Slot {
    public CraftInputSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }
}
