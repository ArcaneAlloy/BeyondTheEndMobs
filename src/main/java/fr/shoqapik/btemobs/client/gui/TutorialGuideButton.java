package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Botón pequeño del diálogo de un NPC que abre su guía ilustrada (el tutorial de ese NPC).
 *
 * Si existe la textura textures/gui/buttons/tutorial.png (SIZE x SIZE; la mitad de abajo, si la imagen mide el doble de
 * alto, se usa al pasar el ratón) se dibuja esa imagen; si no, el icono "?" del botón "Guide me!" de Antonio
 * (textures/gui/buttons/explorer/rumor.png), con un brillo suave al pasar el ratón.
 */
public class TutorialGuideButton extends Button {

    public static final int SIZE = 20;
    public static final ResourceLocation TEXTURE = new ResourceLocation(BteMobsMod.MODID, "textures/gui/buttons/tutorial.png");
    /** Icono "?" del botón "Guide me!" de Antonio (15 x 15). */
    public static final ResourceLocation ICON = new ResourceLocation(BteMobsMod.MODID, "textures/gui/buttons/explorer/rumor.png");
    private static final int ICON_SIZE = 15;

    private static Boolean hasTexture = null;
    private static int textureHeight = SIZE;

    public TutorialGuideButton(int x, int y, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.literal("?"), onPress);
    }

    private static boolean hasTexture() {
        if (hasTexture == null) {
            var resource = Minecraft.getInstance().getResourceManager().getResource(TEXTURE);
            hasTexture = resource.isPresent();
            if (hasTexture) {
                try (var in = resource.get().open(); var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                    textureHeight = image.getHeight();
                } catch (Exception e) {
                    textureHeight = SIZE;
                }
            }
        }
        return hasTexture;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!hasTexture()) {
            renderIcon(poseStack);
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int v = this.isHoveredOrFocused() && textureHeight >= SIZE * 2 ? SIZE : 0;
        GuiComponent.blit(poseStack, this.x, this.y, 0, v, SIZE, SIZE, SIZE, textureHeight);
    }

    /** Icono "?" centrado en el botón; al pasar el ratón, un halo claro detrás y el icono un poco más grande. */
    private void renderIcon(PoseStack poseStack) {
        boolean hover = this.isHoveredOrFocused();
        if (hover) {
            GuiComponent.fill(poseStack, this.x + 1, this.y + 1, this.x + SIZE - 1, this.y + SIZE - 1, 0x40FFFFFF);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ICON);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        poseStack.pushPose();
        float scale = hover ? 1.2F : 1.0F;
        poseStack.translate(this.x + SIZE / 2.0F, this.y + SIZE / 2.0F, 0);
        poseStack.scale(scale, scale, 1.0F);
        GuiComponent.blit(poseStack, -ICON_SIZE / 2, -ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        poseStack.popPose();
    }

    /** La textura se vuelve a buscar al recargar los recursos (F3+T). */
    public static void resetTextureCache() {
        hasTexture = null;
    }
}
