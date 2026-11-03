package fr.shoqapik.btemobs.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import fr.shoqapik.btemobs.api.QuestRecipeLinks;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;

/**
 * Franja "Requiere: <quest> (<NPC>)" debajo de las recetas de JEI que siguen bloqueadas por una quest, igual que el
 * aviso de la interfaz de reparar de la herrera:
 *  - si el texto no cabe, va de un lado a otro;
 *  - el nombre de la quest es un enlace que la abre en el libro de quests (al cerrarlo se vuelve a JEI);
 *  - si la quest aún no es visible para el jugador (le falta algún requisito), su nombre sale censurado y no es un
 *    enlace (sería un spoiler).
 * Cuando el jugador ya ha reclamado la quest la franja desaparece.
 */
public final class QuestLockStrip {

    private QuestLockStrip() {}

    /** Hueco que se añade debajo del fondo de la categoría: 2 px de separación + 12 px de franja. */
    public static final int GAP = 2;
    public static final int STRIP_H = 12;
    public static final int EXTRA_HEIGHT = GAP + STRIP_H;
    /** Margen lateral de la franja dentro del ancho de la categoría y relleno del texto. */
    private static final int MARGIN = 4;
    private static final int PADDING = 3;

    private static final ResourceLocation ALT_FONT = new ResourceLocation("minecraft", "alt");

    /** El fondo de la categoría con el hueco de la franja debajo (el fondo se dibuja igual que antes). */
    public static IDrawable extend(IDrawable background) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return background.getWidth();
            }

            @Override
            public int getHeight() {
                return background.getHeight() + EXTRA_HEIGHT;
            }

            @Override
            public void draw(PoseStack poseStack, int xOffset, int yOffset) {
                background.draw(poseStack, xOffset, yOffset);
            }
        };
    }

    /** Quest que el jugador aún tiene que reclamar para desbloquear esta receta, o null. */
    @Nullable
    public static String questForRecipe(ResourceLocation recipeId) {
        String questId = QuestRecipeLinks.questForRecipe(recipeId);
        if (questId == null || QuestRecipeLinks.isQuestClaimed(questId)) return null;
        return questId;
    }

    /** Quest que el jugador aún tiene que reclamar para poder reparar este objeto en la herrera, o null. */
    @Nullable
    public static String questForRepair(Item item) {
        return QuestRecipeLinks.questForRepair(item);
    }

    /** Posiciones de la franja en este frame (coordenadas de la receta). */
    private static final class Layout {
        Component prefix;
        Component title;
        boolean openable;
        int y;
        int left;
        int right;
        int startX;
        int nameX0;
        int nameX1;

        boolean overName(double mouseX, double mouseY) {
            return openable && mouseX >= Math.max(nameX0, left) && mouseX < Math.min(nameX1, right)
                    && mouseY >= y && mouseY < y + STRIP_H;
        }
    }

    private static Layout layout(String questId, int width, int backgroundHeight) {
        Font font = Minecraft.getInstance().font;
        Layout l = new Layout();
        Component title = QuestRecipeLinks.questTitle(questId);
        l.title = title == null ? Component.literal(questId) : title;
        l.openable = QuestRecipeLinks.questRequirements(questId).isEmpty() && QuestRecipeLinks.canOpenQuest(questId);
        l.prefix = Component.translatable("gui.bte_mobs.repair.locked").append(" ");
        l.y = backgroundHeight + GAP;
        l.left = MARGIN + PADDING;
        l.right = width - MARGIN - PADDING;
        int inner = l.right - l.left;

        Component plainName = l.openable ? l.title : censor(l.title.getString());
        int prefixW = font.width(l.prefix);
        int textW = prefixW + font.width(plainName);
        if (textW <= inner) {
            l.startX = l.left + (inner - textW) / 2;
        } else {
            // No cabe: el texto va de un lado a otro (ida y vuelta, con pausa en cada extremo)
            int overflow = textW - inner;
            double seconds = net.minecraft.Util.getMillis() / 1000.0D;
            double period = Math.max(overflow * 0.5D, 3.0D);
            double t = Math.sin(Math.PI / 2.0D * Math.cos(Math.PI * 2.0D * seconds / period)) / 2.0D + 0.5D;
            l.startX = l.left - (int) Math.round(t * overflow);
        }
        l.nameX0 = l.startX + prefixW;
        l.nameX1 = l.startX + textW;
        return l;
    }

    private static Component censor(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(ALT_FONT)
                .applyFormat(ChatFormatting.OBFUSCATED).applyFormat(ChatFormatting.RED));
    }

    /**
     * Dibuja la franja (si questId no es null). width y backgroundHeight son los del fondo original de la categoría;
     * mouseX/mouseY, los que JEI pasa a draw (relativos a la receta).
     */
    public static void draw(PoseStack poseStack, @Nullable String questId, int width, int backgroundHeight,
                            double mouseX, double mouseY) {
        if (questId == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Layout l = layout(questId, width, backgroundHeight);
        boolean hover = l.overName(mouseX, mouseY);

        Component name = l.openable
                ? l.title.copy().withStyle(hover ? ChatFormatting.YELLOW : ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                : censor(l.title.getString());

        GuiComponent.fill(poseStack, MARGIN, l.y, width - MARGIN, l.y + STRIP_H, hover ? 0xC0330044 : 0xA0000000);

        // Recorte a la franja: JEI ya ha desplazado la receta con el PoseStack, así que la posición en pantalla se
        // saca de la matriz
        Matrix4f pose = poseStack.last().pose();
        Vector4f topLeft = new Vector4f(l.left, l.y, 0.0F, 1.0F);
        topLeft.transform(pose);
        Vector4f bottomRight = new Vector4f(l.right, l.y + STRIP_H, 0.0F, 1.0F);
        bottomRight.transform(pose);
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        double scale = window.getGuiScale();
        int sx = (int) (topLeft.x() * scale);
        int sy = (int) (window.getHeight() - bottomRight.y() * scale);
        int sw = (int) ((bottomRight.x() - topLeft.x()) * scale);
        int sh = (int) ((bottomRight.y() - topLeft.y()) * scale);
        RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
        font.drawShadow(poseStack, l.prefix, (float) l.startX, (float) (l.y + 2), 0xFF6060);
        font.drawShadow(poseStack, name, (float) l.nameX0, (float) (l.y + 2), 0xFF6060);
        RenderSystem.disableScissor();
    }

    /** Clic izquierdo sobre el nombre de la quest: la abre en el libro de quests (al cerrarlo se vuelve a JEI). */
    public static boolean handleInput(@Nullable String questId, int width, int backgroundHeight,
                                      double mouseX, double mouseY, InputConstants.Key input) {
        if (questId == null) return false;
        if (input.getType() != InputConstants.Type.MOUSE || input.getValue() != 0) return false;
        Layout l = layout(questId, width, backgroundHeight);
        if (!l.overName(mouseX, mouseY)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (QuestRecipeLinks.openQuest(questId, mc.screen)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            return true;
        }
        return false;
    }
}
