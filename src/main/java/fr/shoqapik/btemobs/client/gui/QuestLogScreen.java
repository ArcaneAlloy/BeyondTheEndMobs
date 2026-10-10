package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.client.QuestReadyClient;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.Quest;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Libro de quests (botón del inventario): elige un NPC y abre sus quests en modo consulta.
 * Desde aquí no se puede reclamar nada: hay que ir al NPC.
 */
public class QuestLogScreen extends Screen {

    private static final BteNpcType[] NPCS = {BteNpcType.BLACKSMITH, BteNpcType.EXPLORER, BteNpcType.WARLOCK, BteNpcType.DRUID};
    private static final int BUTTON_W = 160;
    private static final int BUTTON_H = 32;
    /** Cara del NPC: textura de 56x56 dibujada a 28x28 (nítida con escala de interfaz 2 y 4). */
    private static final int FACE_SIZE = 28;
    private static final int FACE_TEX = 56;
    private static final int GAP = 6;

    @Nullable
    private final Screen parent;

    public QuestLogScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.bte_mobs.quest_log.title"));
        this.parent = parent;
    }

    public static ItemStack icon(BteNpcType npc) {
        return switch (npc) {
            case BLACKSMITH -> new ItemStack(Items.ANVIL);
            case EXPLORER -> new ItemStack(Items.COMPASS);
            case WARLOCK -> new ItemStack(Items.ENCHANTED_BOOK);
            case DRUID -> new ItemStack(Items.OAK_SAPLING);
            default -> new ItemStack(Items.WRITABLE_BOOK);
        };
    }

    /** Cara del NPC para su botón (textures/gui/quest_log/<npc>.png). */
    public static ResourceLocation face(BteNpcType npc) {
        String name = switch (npc) {
            case BLACKSMITH -> "anna";
            case EXPLORER -> "antonio";
            case WARLOCK, NPC5 -> "noah";
            case DRUID -> "oriana";
            default -> null;
        };
        return name == null ? null : new ResourceLocation(BteMobsMod.MODID, "textures/gui/quest_log/" + name + ".png");
    }

    @Override
    protected void init() {
        int total = NPCS.length * BUTTON_H + (NPCS.length - 1) * GAP;
        int x = (this.width - BUTTON_W) / 2;
        int y = (this.height - total) / 2 + 6;
        for (BteNpcType npc : NPCS) {
            Component name = Component.translatable("entity.bte_mobs." + npc.name().toLowerCase(Locale.ROOT));
            this.addRenderableWidget(new Button(x, y, BUTTON_W, BUTTON_H, name,
                    b -> this.minecraft.setScreen(new QuestScreen(npc, this))) {
                @Override
                public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                    super.renderButton(poseStack, mouseX, mouseY, partialTick);
                    ResourceLocation face = face(npc);
                    if (face != null) {
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.setShaderTexture(0, face);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        blit(poseStack, this.x + 4, this.y + (this.height - FACE_SIZE) / 2, FACE_SIZE, FACE_SIZE,
                                0, 0, FACE_TEX, FACE_TEX, FACE_TEX, FACE_TEX);
                        RenderSystem.disableBlend();
                    } else {
                        minecraft.getItemRenderer().renderAndDecorateFakeItem(icon(npc), this.x + 4, this.y + (this.height - 16) / 2);
                    }
                    // Progreso: quests reclamadas / total
                    String progress = progress(npc);
                    font.drawShadow(poseStack, progress, this.x + this.width - 6 - font.width(progress), this.y + (this.height - 8) / 2, 0xAAAAAA);
                    if (QuestReadyClient.hasReady(npc)) {
                        font.drawShadow(poseStack, "!", this.x + 6 + FACE_SIZE, this.y + (this.height - 8) / 2, 0xFFD700);
                    }
                }
            });
            y += BUTTON_H + GAP;
        }
    }

    private String progress(BteNpcType npc) {
        if (this.minecraft == null || this.minecraft.player == null) return "";
        RecipeCapability<?> cap = RecipeCapability.get(this.minecraft.player);
        if (cap == null) return "";
        Map<Quest, QuestStateData> quests = cap.getQuestForNpc(npc);
        if (quests == null || quests.isEmpty()) return "";
        long done = quests.values().stream().filter(s -> s.isReclaim).count();
        return done + "/" + quests.size();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        int total = NPCS.length * BUTTON_H + (NPCS.length - 1) * GAP;
        int top = (this.height - total) / 2 + 6;
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, top - 24, 0xFFD700);
        drawCenteredString(poseStack, this.font, Component.translatable("gui.bte_mobs.quest_log.hint"), this.width / 2, top - 12, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
