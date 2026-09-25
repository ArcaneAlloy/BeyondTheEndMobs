package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.ToggleCheckbox;
import fr.shoqapik.btemobs.config.BteMobsClientConfig;
import fr.shoqapik.btemobs.menu.BlacksmithCraftMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlacksmithCraftScreen extends BteAbstractCraftScreen<BlacksmithCraftMenu> {

    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_screen.png");

    private static final Component NO_ANIMATIONS_LABEL = Component.translatable("gui.bte_mobs.blacksmith.no_animations");
    private static final Component NO_ANIMATIONS_TOOLTIP = Component.translatable("gui.bte_mobs.blacksmith.no_animations.tooltip");

    private ToggleCheckbox noAnimationsCheckbox;

    public BlacksmithCraftScreen(BlacksmithCraftMenu containerMenu, Inventory inventory, Component component) {
        super(containerMenu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        // Hueco libre de la textura: a la izquierda del boton Craft, misma fila.
        this.noAnimationsCheckbox = this.addRenderableWidget(new ToggleCheckbox(
                this.leftPos + 8,
                this.topPos + 70,
                NO_ANIMATIONS_LABEL,
                BteMobsClientConfig.BLACKSMITH_SKIP_CRAFT_ANIMATION.get(),
                BteMobsClientConfig.BLACKSMITH_SKIP_CRAFT_ANIMATION::set
        ));
    }

    @Override
    protected boolean shouldSkipCraftAnimation() {
        return this.noAnimationsCheckbox != null && this.noAnimationsCheckbox.isSelected();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.noAnimationsCheckbox != null && this.noAnimationsCheckbox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        if (this.noAnimationsCheckbox != null && this.noAnimationsCheckbox.visible
                && this.noAnimationsCheckbox.isMouseOver(mouseX, mouseY)) {
            this.renderTooltip(poseStack, this.font.split(NO_ANIMATIONS_TOOLTIP, 200), mouseX, mouseY);
        }
    }

    @Override
    public ResourceLocation getTexture() {
        return CRAFTING_TABLE_LOCATION;
    }
}
