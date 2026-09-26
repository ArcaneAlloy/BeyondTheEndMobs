package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * Checkbox compacta (10x10) con etiqueta, pensada para encajar en las GUIs
 * oscuras de los NPCs. El texto usa el mismo estilo que el boton "Craft".
 */
@OnlyIn(Dist.CLIENT)
public class ToggleCheckbox extends AbstractButton {

    private static final int BOX = 10;
    private static final int LABEL_GAP = 4;

    private static final int COLOR_BORDER = 0xFF8B8B8B;
    private static final int COLOR_BORDER_HOVER = 0xFFFFFFFF;
    private static final int COLOR_INNER = 0xFF15181A;
    private static final int COLOR_CHECK = 0xFF5CC85C;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_TEXT_HOVER = 0xFFFFA0;

    // Forma del check (x, y relativos a la caja); cada punto se pinta 1x2.
    private static final int[][] CHECK_PIXELS = {{2, 4}, {3, 5}, {4, 6}, {5, 5}, {6, 4}, {7, 3}};

    private final Consumer<Boolean> onToggle;
    private boolean selected;

    public ToggleCheckbox(int x, int y, Component label, boolean selected, Consumer<Boolean> onToggle) {
        super(x, y, BOX + LABEL_GAP + Minecraft.getInstance().font.width(label), BOX, label);
        this.selected = selected;
        this.onToggle = onToggle;
    }

    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
        this.onToggle.accept(this.selected);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hovered = this.isHoveredOrFocused();
        int x0 = this.x;
        int y0 = this.y;

        fill(poseStack, x0, y0, x0 + BOX, y0 + BOX, hovered ? COLOR_BORDER_HOVER : COLOR_BORDER);
        fill(poseStack, x0 + 1, y0 + 1, x0 + BOX - 1, y0 + BOX - 1, COLOR_INNER);

        if (this.selected) {
            for (int[] p : CHECK_PIXELS) {
                fill(poseStack, x0 + p[0], y0 + p[1], x0 + p[0] + 1, y0 + p[1] + 2, COLOR_CHECK);
            }
        }

        font.drawShadow(poseStack, this.getMessage(), x0 + BOX + LABEL_GAP, y0 + 1, hovered ? COLOR_TEXT_HOVER : COLOR_TEXT);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
