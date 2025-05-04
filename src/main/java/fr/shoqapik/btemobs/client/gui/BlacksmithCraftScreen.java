package fr.shoqapik.btemobs.client.gui;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.BlacksmithCraftMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public class BlacksmithCraftScreen extends BteAbstractCraftScreen<BlacksmithCraftMenu> {

    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_screen.png");

    public BlacksmithCraftScreen(BlacksmithCraftMenu containerMenu, Inventory inventory, Component component) {
        super(containerMenu, inventory, component);
    }

    @Override
    public ResourceLocation getTexture() {
        return CRAFTING_TABLE_LOCATION;
    }
}
