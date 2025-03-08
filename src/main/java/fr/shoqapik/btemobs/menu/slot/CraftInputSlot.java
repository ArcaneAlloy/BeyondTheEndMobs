package fr.shoqapik.btemobs.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraftInputSlot extends Slot {
    public CraftInputSlot(Container p_40223_, int p_40224_, int p_40225_, int p_40226_) {
        super(p_40223_, p_40224_, p_40225_, p_40226_);
    }

    @Override
    public boolean mayPlace(ItemStack p_40231_) {
        return true;
    }

    @Override
    public boolean mayPickup(Player p_40228_) {
        return true;
    }
}
