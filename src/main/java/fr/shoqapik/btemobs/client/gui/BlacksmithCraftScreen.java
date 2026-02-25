package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.BlacksmithCraftMenu;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
