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
 * alto, se usa al pasar el ratón) se dibuja esa imagen; si no, un botón normal de Minecraft con un "?".
 */
public class TutorialGuideButton extends Button {

    public static final int SIZE = 20;
    public static final ResourceLocation TEXTURE = new ResourceLocation(BteMobsMod.MODID, "textures/gui/buttons/tutorial.png");

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
            super.renderButton(poseStack, mouseX, mouseY, partialTick);
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

    /** La textura se vuelve a buscar al recargar los recursos (F3+T). */
    public static void resetTextureCache() {
        hasTexture = null;
    }
}
