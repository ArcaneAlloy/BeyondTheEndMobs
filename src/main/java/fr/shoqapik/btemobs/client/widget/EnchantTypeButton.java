
package fr.shoqapik.btemobs.client.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantTypeButton extends AbstractWidget {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");

    private Minecraft minecraft = Minecraft.getInstance();
    private Recipe<?> recipe;
    private Enchantment enchantment;
    private ItemStack recipeStack;
    public Component typeEnchant = Component.empty();
    public boolean hasEnough = false;
    public EnchantTypeButton(int x, int y, Enchantment enchant) {
        super(x, y, 25, 25, CommonComponents.EMPTY);
        this.enchantment = enchant;
        this.recipeStack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchant,0));
    }

    public EnchantTypeButton(int p_93629_, int p_93630_, int p_93631_, int p_93632_, Component p_93633_) {
        super(p_93629_, p_93630_, p_93631_, p_93632_, p_93633_);
    }



    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }

    @Override
    public void renderButton(PoseStack p_93676_, int p_93677_, int p_93678_, float p_93679_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION);
        int i = 54;
        int j = 205;
        if(hasEnough){
            i -= 25;
        }
        if(!isHoveredOrFocused()){
            j -= 25;
        }
        blit(p_93676_, this.x, this.y, i, j, this.width, this.height, 512, 512);

        minecraft.getItemRenderer().renderAndDecorateItem(recipeStack, this.x + 4, this.y + 4, 0, 10);
    }

    public void setPosition(int x, int y){
        this.x = x;
        this.y = y;
    }

    public ItemStack getRecipeStack() {
        return recipeStack;
    }

    public Component getTypeEnchant(){
        return this.typeEnchant;
    }

    public List<Component> getTooltipText(Screen p_100478_) {
        ItemStack itemstack = this.recipeStack;
        List<Component> list = Lists.newArrayList(p_100478_.getTooltipFromItem(itemstack));
        list.remove(enchantment.getFullname(0));
        if(list.size() > 1) list.add(Component.literal(""));
        list.add(getTypeEnchant());
        return list;
    }

    public void setHasEnough(boolean hasEnough) {
        this.hasEnough = hasEnough;
    }


    public Recipe<?> getRecipe() {
        return recipe;
    }
}
