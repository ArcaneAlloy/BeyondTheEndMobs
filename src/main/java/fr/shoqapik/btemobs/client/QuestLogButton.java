package fr.shoqapik.btemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.gui.QuestLogScreen;
import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Botón en el inventario (a la izquierda, debajo del botón que ya hay) que abre el libro de quests de los NPCs.
 * Se mueve con el inventario cuando se abre o cierra el libro de recetas.
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestLogButton {

    private QuestLogButton() {}

    /** Posición respecto a la esquina superior izquierda del inventario. */
    private static final int OFFSET_X = -25;
    private static final int OFFSET_Y = 30;
    private static final int SIZE = 20;

    private static final ItemStack ICON = new ItemStack(Items.WRITABLE_BOOK);

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        event.addListener(new LogButton(screen));
    }

    private static class LogButton extends Button {
        private final InventoryScreen screen;

        LogButton(InventoryScreen screen) {
            super(screen.getGuiLeft() + OFFSET_X, screen.getGuiTop() + OFFSET_Y, SIZE, SIZE, Component.empty(),
                    b -> Minecraft.getInstance().setScreen(new QuestLogScreen(screen)),
                    (button, poseStack, mouseX, mouseY) -> screen.renderComponentTooltip(poseStack,
                            List.of(Component.translatable("gui.bte_mobs.quest_log.title")), mouseX, mouseY));
            this.screen = screen;
        }

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            // Sigue al inventario (el libro de recetas lo desplaza)
            this.x = screen.getGuiLeft() + OFFSET_X;
            this.y = screen.getGuiTop() + OFFSET_Y;
            super.renderButton(poseStack, mouseX, mouseY, partialTick);
            Minecraft mc = Minecraft.getInstance();
            mc.getItemRenderer().renderAndDecorateFakeItem(ICON, this.x + 2, this.y + 2);
            // "!" dorado si algún NPC tiene una quest lista para reclamar
            for (BteNpcType npc : BteNpcType.values()) {
                if (QuestReadyClient.hasReady(npc)) {
                    poseStack.pushPose();
                    poseStack.translate(0, 0, 300); // por encima del item
                    mc.font.drawShadow(poseStack, "!", this.x + SIZE - 5, this.y + 1, 0xFFD700);
                    poseStack.popPose();
                    break;
                }
            }
        }
    }
}
