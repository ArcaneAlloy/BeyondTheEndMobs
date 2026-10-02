package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Aviso en la esquina (como los logros): "Quest lista para reclamar" + título de la quest + NPC al que ir.
 */
public class QuestReadyToast implements Toast {

    private static final long DISPLAY_TIME = 6000L;

    private final Component questTitle;
    private final BteNpcType npc;
    private final ItemStack icon;

    public QuestReadyToast(Component questTitle, BteNpcType npc) {
        this.questTitle = questTitle;
        this.npc = npc;
        this.icon = iconFor(npc);
    }

    private static ItemStack iconFor(BteNpcType npc) {
        if (npc == null) return new ItemStack(Items.WRITABLE_BOOK);
        return switch (npc) {
            case BLACKSMITH -> new ItemStack(Items.ANVIL);
            case EXPLORER -> new ItemStack(Items.COMPASS);
            case WARLOCK -> new ItemStack(Items.ENCHANTED_BOOK);
            case DRUID -> new ItemStack(Items.OAK_SAPLING);
            default -> new ItemStack(Items.WRITABLE_BOOK);
        };
    }

    @Override
    public Visibility render(PoseStack poseStack, ToastComponent toastComponent, long timeSinceLastVisible) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Mismo fondo que los avisos de logros de vanilla
        toastComponent.blit(poseStack, 0, 0, 0, 0, this.width(), this.height());

        Font font = toastComponent.getMinecraft().font;
        String npcKey = npc == null ? "" : "entity.bte_mobs." + npc.name().toLowerCase(java.util.Locale.ROOT);
        Component header = Component.translatable("toast.bte_mobs.quest_ready", Component.translatable(npcKey));
        font.draw(poseStack, header, 30.0F, 7.0F, 0xFFFF55);

        String title = questTitle.getString();
        int max = this.width() - 36;
        if (font.width(title) > max) title = font.plainSubstrByWidth(title, max - font.width("…")) + "…";
        font.draw(poseStack, title, 30.0F, 18.0F, 0xFFFFFF);

        toastComponent.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(this.icon, 8, 8);
        return timeSinceLastVisible >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }
}
