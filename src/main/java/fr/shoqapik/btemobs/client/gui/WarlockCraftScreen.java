package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.WarlockCraftMenu;
import fr.shoqapik.btemobs.packets.LastClickedRecipeUpdatePacket;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Iterator;


public class WarlockCraftScreen extends BteAbstractCraftScreen<WarlockCraftMenu> {

    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_screen.png");

    public WarlockCraftScreen(WarlockCraftMenu containerMenu, Inventory inventory, Component component) {
        super(containerMenu, inventory, component);
    }

    @Override
    public ResourceLocation getTexture() {
        return CRAFTING_TABLE_LOCATION;
    }

    @Override
    protected void init() {
        super.init();
        this.craftButton.visible = false;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        if(this.menu.experience.get() > 0) {
            this.renderExperience(pPoseStack);
        }

        /*this.recipeBookComponent.recipeBookPage.getRecipeBook();
        if (this.minecraft.screen != null && !this.recipeBookComponent.recipeBookPage.overlay.isVisible()) {
            for(RecipeButton button : this.recipeBookComponent.recipeBookPage.buttons) {
                if(button.getCollection() == null) continue;
                if(button.getRecipe() instanceof WarlockRecipe) {
                    WarlockRecipe recipe = (WarlockRecipe) button.getRecipe();
                    pPoseStack.pushPose();
                    pPoseStack.translate(button.x, button.y, 1000);
                    pPoseStack.scale(1.3F, 1.3F, 1.3F);
                    Component component = Component.literal(toRoman(recipe.getLevel())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                    float textWidth = this.font.width(component);
                    float centerX = (button.getWidth() / 2.0F + 2) - (textWidth / 2.0F);
                    this.font.draw(pPoseStack, component, centerX, -1F, 0xFFFFFF);
                    pPoseStack.popPose();
                }
            }
        }*/
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.minecraft.screen != null && !this.recipeBookComponent.recipeBookPage.overlay.isVisible()) {
            Iterator var6 = this.recipeBookComponent.recipeBookPage.buttons.iterator();

            while(var6.hasNext()) {
                RecipeButton button = (RecipeButton)var6.next();
                if (button.mouseClicked(pMouseX, pMouseY, pButton)) {
                    BteMobsMod.sendToServer(new LastClickedRecipeUpdatePacket(button.getRecipe()));
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void renderExperience(PoseStack pPoseStack) {
        Component component = Component.literal("Enchantment Cost: " + this.menu.experience.get());
        int x = this.imageWidth - this.font.width(component) + 132;
        int color = 8453920;
        if(this.minecraft.player.experienceLevel < this.menu.experience.get()) {
            color = 16736352;
        }
        this.font.drawShadow(pPoseStack, component, x, 98, color);
    }

    private static String toRoman(int input) {
        if (input < 1 || input > 3999)
            return "";
        StringBuilder s = new StringBuilder();
        while (input >= 1000) {
            s.append("M");
            input -= 1000;        }
        while (input >= 900) {
            s.append("CM");
            input -= 900;
        }
        while (input >= 500) {
            s.append("D");
            input -= 500;
        }
        while (input >= 400) {
            s.append("CD");
            input -= 400;
        }
        while (input >= 100) {
            s.append("C");
            input -= 100;
        }
        while (input >= 90) {
            s.append("XC");
            input -= 90;
        }
        while (input >= 50) {
            s.append("L");
            input -= 50;
        }
        while (input >= 40) {
            s.append("XL");
            input -= 40;
        }
        while (input >= 10) {
            s.append("X");
            input -= 10;
        }
        while (input == 9) {
            s.append("IX");
            input -= 9;
        }
        while (input >= 5) {
            s.append("V");
            input -= 5;
        }
        while (input == 4) {
            s.append("IV");
            input -= 4;
        }
        while (input >= 1) {
            s.append("I");
            input -= 1;
        }
        return s.toString();
    }
}
