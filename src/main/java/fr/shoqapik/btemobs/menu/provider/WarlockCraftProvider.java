package fr.shoqapik.btemobs.menu.provider;

import fr.shoqapik.btemobs.menu.WarlockCraftMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jetbrains.annotations.Nullable;

public class WarlockCraftProvider implements MenuProvider {

    private final int entityId;

    public WarlockCraftProvider(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Warlock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory p_39955_, Player p_39956_) {
        return new WarlockCraftMenu(p_39954_, p_39956_.getInventory(), entityId);
    }
}
