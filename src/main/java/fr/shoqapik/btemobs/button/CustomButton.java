package fr.shoqapik.btemobs.button;

import fr.shoqapik.btemobs.BteMobsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomButton extends Button {
    public ResourceLocation texture;
    private final ResourceLocation texture2;
    public boolean hasColor = false;
    public ItemStack item = ItemStack.EMPTY;
    private boolean isLock=false;
    public boolean isSelect = true;
    public List<Component> listComponents = new ArrayList<>();
    public float[] color = new float[]{
            1.0F,1.0F,1.0F,1.0F
    };
    public CustomButton(ResourceLocation texture, ResourceLocation texture2, int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
        this.texture = texture;
        this.texture2 = texture2;

    }
    public CustomButton(ResourceLocation texture, ResourceLocation texture2, int x, int y, int width, int height, Component message,List<Component> components, OnPress onPress) {
        super(x, y, width, height, message, onPress);
        this.texture = texture;
        this.texture2 = texture2;
        listComponents = components;
    }
    public CustomButton(ResourceLocation texture, ResourceLocation texture2, int x, int y, int width, int height,float[] color, Component message,List<Component> components, OnPress onPress) {
        super(x, y, width, height, message, onPress);
        this.texture = texture;
        this.texture2 = texture2;
        listComponents = components;
        this.hasColor = color.length>0;
        this.color = color;
    }

    public CustomButton(ResourceLocation texture, ResourceLocation texture2, int x, int y, int width, int height, Component message, OnPress onPress,Button.OnTooltip onTooltip) {
        super(x, y, width, height, message, onPress,onTooltip);
        this.texture = texture;
        this.texture2 = texture2;
    }


    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, texture);


        if (this.isHovered) {
            RenderSystem.setShaderColor(color[0] * 0.7F, color[1]* 0.7F, color[2] *0.7F, 1.0f); // Oscurece (70% de brillo)
        } else {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]); // Color normal
        }

        if(this.isLock || !isSelect){
            RenderSystem.setShaderColor(color[0] * 0.3F, color[1] * 0.3F, color[2] * 0.3F, 1.0f); // Color normal
        }

        blit(poseStack, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (texture2 != null) {
            int iconWidth = this.height/3 * 2;
            int iconHeight = this.height/3 * 2;
            int iconX = this.x + (this.width - iconWidth);
            int iconY = this.y + (this.height - iconHeight);


            RenderSystem.setShaderTexture(0, texture2);
            blit(poseStack, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
        }

        if(this.isLock){
            ResourceLocation foregroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/explorer/candado.png"));
            int iconWidth = this.height/3 * 2;
            int iconHeight = this.height/3 * 2;
            int iconX = this.x + this.width/2 - iconWidth/2;
            int iconY = this.y + this.height/2 - iconHeight/2;


            RenderSystem.setShaderTexture(0, foregroundTexture);
            blit(poseStack, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
            if (this.isMouseOver(mouseX,mouseY)) {
                this.renderToolTip(poseStack,mouseX, mouseY);
            }
        } else {
            drawCenteredString(poseStack, Minecraft.getInstance().font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFFFFF);
        }
    }

    @Override
    public void renderToolTip(PoseStack p_93736_, int p_93737_, int p_93738_) {
        super.renderToolTip(p_93736_, p_93737_, p_93738_);

        if (!this.listComponents.isEmpty()){
            Minecraft.getInstance().screen.renderComponentTooltip(p_93736_, this.listComponents,p_93737_,p_93738_);
        }
    }

    public void setIsLock(boolean flag){
        this.isLock=flag;
    }

}
