package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.renderer.GameRenderer;

public class SmithStateSwitchingButton extends StateSwitchingButton {

    public SmithStateSwitchingButton(int p_94615_, int p_94616_, int p_94617_, int p_94618_, boolean p_94619_) {
        super(p_94615_, p_94616_, p_94617_, p_94618_, p_94619_);
    }

    @Override
    public void renderButton(PoseStack p_94631_, int p_94632_, int p_94633_, float p_94634_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        RenderSystem.disableDepthTest();
        int i = this.xTexStart;
        int j = this.yTexStart;
        if (this.isStateTriggered) {
            i += this.xDiffTex;
        }

        if (this.isHoveredOrFocused()) {
            j += this.yDiffTex;
        }

        blit(p_94631_, this.x, this.y, i, j, this.width, this.height, 512, 512);
        RenderSystem.enableDepthTest();
    }



}
