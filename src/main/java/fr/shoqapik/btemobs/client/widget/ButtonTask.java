package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.StatTaskData;
import fr.shoqapik.btemobs.quest.TaskData;
import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ButtonTask extends Button {
    public StatTaskData data;
    public ItemStack item;
    public Minecraft minecraft;
    public ButtonTask(int p_93721_, int p_93722_, int p_93723_, int p_93724_, StatTaskData data, Minecraft minecraft) {
        super(p_93721_, p_93722_, p_93723_, p_93724_, Component.empty(),(button)->{},(s,p1,p2,p3)->{

        });
        this.data = data;
        this.minecraft = minecraft;
        this.item = getItemOfTaskData();

    }

    @Override
    public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
        super.render(p_93657_, p_93658_, p_93659_, p_93660_);
        if (data.taskType == TaskData.Type.COLLECT) {
            renderItem(p_93657_);
        } else {
            renderTexture(p_93657_);
        }
    }

    @Override
    public void renderToolTip(PoseStack p_93736_, int p_93737_, int p_93738_) {
        super.renderToolTip(p_93736_, p_93737_, p_93738_);
        if (this.item.isEmpty()){
            this.minecraft.screen.renderComponentTooltip(p_93736_, getMessageForType(),p_93737_,p_93738_);
        }else {
            this.minecraft.screen.renderComponentTooltip(p_93736_, this.minecraft.screen.getTooltipFromItem(item), p_93737_, p_93738_, item);
        }

    }

    private List<Component> getMessageForType() {
        return List.of(Component.literal(this.data.description));
    }

    public boolean isMouseOver(double p_93672_, double p_93673_) {
        return p_93672_ >= (double)this.x && p_93673_ >= (double)this.y && p_93672_ < (double)(this.x + this.width) && p_93673_ < (double)(this.y + this.height);
    }

    public ItemStack getItemOfTaskData(){
        ItemStack stack;
        switch (data.taskType){
            case COLLECT -> {
                stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.id)));
            }
            default -> {
                stack = ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private void renderItem(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();

        ItemRenderer renderer = mc.getItemRenderer();
        ItemStack stack = item;

        renderer.renderAndDecorateItem(stack, this.x, this.y);
        renderer.renderGuiItemDecorations(mc.font, stack, this.x, this.y);
    }

    private void renderTexture(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/warlock/open_craft.png"));

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GuiComponent.blit(poseStack, this.x, this.y, 0, 0, 16, 16, 16, 16);
    }
}
