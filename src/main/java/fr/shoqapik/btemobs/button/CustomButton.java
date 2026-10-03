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
    /** Si es true, el botón no dibuja su tooltip; lo hace la pantalla al final del frame (para que nada lo tape). */
    public boolean deferTooltip = false;
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
            if (!this.deferTooltip && this.isMouseOver(mouseX,mouseY)) {
                this.renderToolTip(poseStack,mouseX, mouseY);
            }
        } else {
            renderScrollingText(poseStack, 0xFFFFFF);
        }
    }

    /** Margen interior (el marco de la textura) dentro del que se dibuja el texto. */
    private static final int TEXT_PADDING = 6;

    /**
     * Texto centrado; si no cabe en el botón, se desplaza de lado a lado (ida y vuelta, con pausa en cada extremo)
     * recortado al interior del botón, como los botones de las versiones nuevas de Minecraft.
     */
    private void renderScrollingText(PoseStack poseStack, int color) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.gui.Font font = mc.font;
        Component text = this.getMessage();
        int textWidth = font.width(text);
        int left = this.x + TEXT_PADDING;
        int right = this.x + this.width - TEXT_PADDING;
        int inner = right - left;
        int textY = this.y + (this.height - 8) / 2;
        if (textWidth <= inner) {
            drawCenteredString(poseStack, font, text, this.x + this.width / 2, textY, color);
            return;
        }
        int overflow = textWidth - inner;
        double seconds = net.minecraft.Util.getMillis() / 1000.0D;
        double period = Math.max(overflow * 0.5D, 3.0D);
        // 0 -> 1 -> 0 suavizado: se para un momento en cada extremo
        double t = Math.sin(Math.PI / 2.0D * Math.cos(Math.PI * 2.0D * seconds / period)) / 2.0D + 0.5D;
        int offset = (int) Math.round(t * overflow);

        // Recorte al interior del botón (coordenadas de ventana)
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        double scale = window.getGuiScale();
        int sx = (int) (left * scale);
        int sy = (int) (window.getHeight() - (this.y + this.height) * scale);
        int sw = (int) (inner * scale);
        int sh = (int) (this.height * scale);
        RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
        drawString(poseStack, font, text, left - offset, textY, color);
        RenderSystem.disableScissor();
    }

    @Override
    public void renderToolTip(PoseStack p_93736_, int p_93737_, int p_93738_) {
        super.renderToolTip(p_93736_, p_93737_, p_93738_);
        if (this.listComponents==null)return;
        if (!this.listComponents.isEmpty()){
            Minecraft.getInstance().screen.renderComponentTooltip(p_93736_, this.listComponents,p_93737_,p_93738_);
        }
    }

    public boolean isLock() {
        return this.isLock;
    }

    public void setIsLock(boolean flag){
        this.isLock=flag;
    }

}
