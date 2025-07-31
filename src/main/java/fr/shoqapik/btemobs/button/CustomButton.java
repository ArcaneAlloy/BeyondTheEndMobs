package fr.shoqapik.btemobs.button;

import fr.shoqapik.btemobs.BteMobsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Locale;

public class CustomButton extends Button {
    private final ResourceLocation texture;
    private final ResourceLocation texture2;
    private boolean isLock=false;

    public CustomButton(ResourceLocation texture, ResourceLocation texture2, int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
        this.texture = texture;
        this.texture2 = texture2;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, texture); // Configura la textura para el renderizado

        if (this.isHovered) {
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1.0f); // Oscurece (70% de brillo)
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Color normal
        }
        if(this.isLock){
            RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f); // Color normal
        }

        blit(poseStack, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (texture2 != null) {
            int iconWidth = this.height/3 * 2;  // Ancho de la segunda textura
            int iconHeight = this.height/3 * 2; // Altura de la segunda textura
            int iconX = this.x + (this.width - iconWidth); // Posición X (en el lado derecho del botón)
            int iconY = this.y + (this.height - iconHeight); // NO Centrado verticalmente

            // Vincula y renderiza la segunda textura (el ícono)
            RenderSystem.setShaderTexture(0, texture2);
            blit(poseStack, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
        }

        // Renderizar el texto
        if(this.isLock){
            ResourceLocation foregroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/explorer/candado.png"));
            int iconWidth = this.height/3 * 2;  // Ancho de la segunda textura
            int iconHeight = this.height/3 * 2; // Altura de la segunda textura
            int iconX = this.x + this.width/2 - iconWidth/2;
            int iconY = this.y + this.height/2 - iconHeight/2;

            // Vincula y renderiza la segunda textura (el ícono)
            RenderSystem.setShaderTexture(0, foregroundTexture);
            blit(poseStack, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);

        }else {
            drawCenteredString(poseStack, Minecraft.getInstance().font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFFFFF);
        }
    }

    public void setIsLock(boolean flag){
        this.isLock=flag;
    }

}
