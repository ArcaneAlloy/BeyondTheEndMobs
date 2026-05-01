package fr.shoqapik.btemobs.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Slot de ingrediente base para el Warlock (encantamiento).
 * Bloquea libros encantados para evitar el crash por NoSuchElementException
 * que ocurria cuando optional.isPresent()==false pero la condicion
 * || baseSlots.getItem(0).is(Items.ENCHANTED_BOOK) era true y se llamaba
 * optional.get() sobre un Optional vacio.
 */
public class WarlockBaseSlot extends Slot {
    public WarlockBaseSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        // No permitir libros encantados — solo libros normales y items encantables
        return !itemStack.is(Items.ENCHANTED_BOOK);
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }
}
