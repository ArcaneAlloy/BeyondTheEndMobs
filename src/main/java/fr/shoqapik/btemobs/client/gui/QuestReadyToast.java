package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Aviso en la esquina (como los logros): "Quest lista · Ve a <NPC>" + título de la quest, con la cara del NPC
 * (la misma que en el libro de quests del inventario).
 */
public class QuestReadyToast implements Toast {

    private static final long DISPLAY_TIME = 6000L;

    /** Ancho del fondo de vanilla y límites del aviso (se ensancha para que quepa el texto). */
    private static final int BASE_WIDTH = 160;
    private static final int MAX_WIDTH = 260;

    /** Cara del NPC: textura de 56x56 dibujada a 28x28 (como en el libro de quests). */
    private static final int FACE_SIZE = 28;
    private static final int FACE_TEX = 56;
    private static final int TEXT_X = 34;

    private final Component header;
    private final Component questTitle;
    private final BteNpcType npc;
    /** Icono de objeto (avisos que no son de un NPC); null para usar la cara del NPC. */
    private final ItemStack icon;
    /** Icono como textura de 16x16 (p. ej. el ojo del botón de Ender Eyes GUI); tiene prioridad sobre el resto. */
    private ResourceLocation iconTexture;
    private final int width;

    public QuestReadyToast(Component questTitle, BteNpcType npc) {
        this(Component.translatable("toast.bte_mobs.quest_ready", Component.translatable(
                npc == null ? "" : "entity.bte_mobs." + npc.name().toLowerCase(Locale.ROOT))), questTitle, npc, null);
    }

    /** Aviso con el mismo estilo pero con cabecera, texto e icono propios (p. ej. el final de la bienvenida). */
    public QuestReadyToast(Component header, Component text, ItemStack icon) {
        this(header, text, (BteNpcType) null, icon);
    }

    /** Aviso con cabecera y texto propios y la cara de un NPC. */
    public static QuestReadyToast withFace(Component header, Component text, BteNpcType npc) {
        return new QuestReadyToast(header, text, npc, null);
    }

    /** Aviso con cabecera y texto propios y un icono de 16x16 dibujado desde una textura. */
    public QuestReadyToast(Component header, Component text, ResourceLocation iconTexture) {
        this(header, text, null, (ItemStack) null);
        this.iconTexture = iconTexture;
    }

    private QuestReadyToast(Component header, Component questTitle, BteNpcType npc, ItemStack icon) {
        this.header = header;
        this.questTitle = questTitle;
        this.npc = npc;
        this.icon = icon;
        Font font = Minecraft.getInstance().font;
        int text = Math.max(font.width(this.header), font.width(questTitle));
        this.width = Math.max(BASE_WIDTH, Math.min(MAX_WIDTH, TEXT_X + text + 8));
    }

    @Override
    public int width() {
        return this.width;
    }

    private static ItemStack fallbackIcon(BteNpcType npc) {
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
        renderBackground(poseStack, toastComponent);

        Font font = toastComponent.getMinecraft().font;
        String header = this.header.getString();
        int max = this.width - TEXT_X - 6;
        if (font.width(header) > max) header = font.plainSubstrByWidth(header, max - font.width("…")) + "…";
        font.draw(poseStack, header, TEXT_X, 7.0F, 0xFFFF55);

        String title = questTitle.getString();
        if (font.width(title) > max) title = font.plainSubstrByWidth(title, max - font.width("…")) + "…";
        font.draw(poseStack, title, TEXT_X, 18.0F, 0xFFFFFF);

        ResourceLocation face = npc == null ? null : QuestLogScreen.face(npc);
        if (this.iconTexture != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, this.iconTexture);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            GuiComponent.blit(poseStack, 9, (this.height() - 16) / 2, 0.0F, 0.0F, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        } else if (face != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, face);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int y = (this.height() - FACE_SIZE) / 2;
            GuiComponent.blit(poseStack, 3, y, FACE_SIZE, FACE_SIZE, 0, 0, FACE_TEX, FACE_TEX, FACE_TEX, FACE_TEX);
            RenderSystem.disableBlend();
        } else {
            ItemStack stack = this.icon != null ? this.icon : fallbackIcon(npc);
            toastComponent.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(stack, 9, 8);
        }
        return timeSinceLastVisible >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }

    /** Fondo de los avisos de logros de vanilla (160 de ancho), estirado por el centro si el aviso es más ancho. */
    private void renderBackground(PoseStack poseStack, ToastComponent toastComponent) {
        int w = this.width;
        int h = this.height();
        if (w <= BASE_WIDTH) {
            toastComponent.blit(poseStack, 0, 0, 0, 0, w, h);
            return;
        }
        int edge = 64;
        toastComponent.blit(poseStack, 0, 0, 0, 0, edge, h);
        for (int x = edge; x < w - edge; x += BASE_WIDTH - 2 * edge) {
            toastComponent.blit(poseStack, x, 0, edge, 0, Math.min(BASE_WIDTH - 2 * edge, w - edge - x), h);
        }
        toastComponent.blit(poseStack, w - edge, 0, BASE_WIDTH - edge, 0, edge, h);
    }
}
