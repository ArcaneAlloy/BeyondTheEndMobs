package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.client.BteMobsModClient;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.ActionPacket;
import fr.shoqapik.btemobs.quests.Quest;
import fr.shoqapik.btemobs.quests.QuestAnswer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestDialogScreen extends Screen {

    public static final ResourceLocation DIALOGS_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/default.png");

    protected int imageWidth = 254;
    protected int imageHeight = 80;
    protected int leftPos;
    protected int topPos;

    private int entityId;
    private BteNpcType bteNpcType;
    private Quest quest;
    private boolean typing;
    private int letterIndex;
    private String currentLine = "";
    private int page;
    private ResourceLocation currentDialogSound;

    private List<Button> buttons = new ArrayList<>();
    private boolean declined;

    public QuestDialogScreen(int entityId, BteNpcType bteNpcType, Quest quest) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quest = quest;
    }


    private String tr(String key) {
        return key != null && key.contains(".") ? I18n.get(key) : key;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int x = this.leftPos - (this.width / 8) + 254;
        int y = this.height - this.imageHeight - 20;

        int index = 0;
        for (QuestAnswer questAnswer : this.quest.getAnswers()) {
            if (index > 3) break;

            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/background.png", bteNpcType.name().toLowerCase(Locale.ROOT)));
            ResourceLocation foregroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s.png", bteNpcType.name().toLowerCase(Locale.ROOT), questAnswer.getAction().toLowerCase(Locale.ROOT)));

            String answerKey = questAnswer.getFormattedAwnser();
            String translatedAnswer = answerKey.contains(".") ? I18n.get(answerKey) : answerKey;

            buttons.add(this.addRenderableWidget(new CustomButton(
                    backgroundTexture, foregroundTexture,
                    x,
                    y + index * 25,
                    100,
                    20,
                    Component.literal(translatedAnswer),
                    (p_95981_) -> {
                        if (questAnswer.getAction().equals("potion") || questAnswer.getAction().equals("rumor")) {
                            Minecraft.getInstance().setScreen(null);
                            BteMobsModClient.handleRumorsPacket(this.entityId);
                        } else if (!questAnswer.getAction().equals("wip")) {
                            Minecraft.getInstance().setScreen(null);
                            BteMobsMod.sendToServer(new ActionPacket(entityId, questAnswer.getAction()));
                            if (BteNpcType.DRUID == this.bteNpcType) {
                                BteMobsModClient.handleClearItem(this.entityId);
                            }
                        } else {
                            Minecraft.getInstance().setScreen(null);
                        }

                        if (currentDialogSound != null) {
                            Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
                            currentDialogSound = null;
                        }
                    }
            )));

            index++;
        }
        setButtonsEnabled(false);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        String rawDialog = this.quest.getDialogs().get(page);
        String translatedDialog = rawDialog.contains(".") ? I18n.get(rawDialog) : rawDialog;

        if (letterIndex < translatedDialog.length()) {
            if (!typing && page < this.quest.getDialogSounds().size()) {

                if (currentDialogSound != null) {
                    Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
                }

                ResourceLocation soundLocation = this.quest.getDialogSounds().get(page);
                double x = Minecraft.getInstance().player.getX();
                double y = Minecraft.getInstance().player.getY();
                double z = Minecraft.getInstance().player.getZ();

                this.minecraft.getSoundManager().play(
                        new SimpleSoundInstance(soundLocation, SoundSource.NEUTRAL, 1.0f, 1.0f,
                                SoundInstance.createUnseededRandom(), false, 0,
                                SoundInstance.Attenuation.LINEAR, x, y, z, false)
                );

                this.currentDialogSound = soundLocation;
            }

            typing = true;
            currentLine += translatedDialog.charAt(letterIndex);
            letterIndex++;
        } else {
            typing = false;
        }

        this.setFocused(null);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID,
                String.format("textures/gui/dialogs/%s.png", bteNpcType.name().toLowerCase(Locale.ROOT))));
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int x = this.leftPos - (this.width / 8);
        int y = this.height - this.imageHeight - 25;

        GuiComponent.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

        drawWordWrap(Component.literal(currentLine), x + 17, y + 30, 220, 16777215, font, poseStack);

        if (!typing) {
            if (page != this.quest.getDialogs().size() - 1 || declined) {
                GuiComponent.drawCenteredString(
                        poseStack,
                        font,
                        tr("gui.bte.continue"),
                        x + imageWidth / 2,
                        y - 13,
                        16777215
                );
            } else {
                setButtonsEnabled(true);
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    public void drawWordWrap(FormattedText text, int x, int y, int width, int color, Font font, PoseStack stack) {
        Matrix4f matrix4f = stack.last().pose();

        for (FormattedCharSequence seq : font.split(text, width)) {
            font.drawInternal(seq, x, y, color, matrix4f, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {

        if (currentDialogSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
            currentDialogSound = null;
        }

        if (typing) {
            typing = false;
            String rawDialog = this.quest.getDialogs().get(page);
            currentLine = rawDialog.contains(".") ? I18n.get(rawDialog) : rawDialog;
            letterIndex = currentLine.length();

        } else if (page != this.quest.getDialogs().size() - 1) {
            page++;
            letterIndex = 0;
            currentLine = "";

        } else if (declined) {
            Minecraft.getInstance().setScreen(null);

        } else {
            return super.mouseClicked(p_94695_, p_94696_, p_94697_);
        }

        return true;
    }

    @Override
    public void removed() {

        if (this.bteNpcType == BteNpcType.NPC5) {
            BteMobsMod.sendToServer(new ActionPacket(entityId, "Discard"));
        }

        if (currentDialogSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
            currentDialogSound = null;
        }

        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void setButtonsEnabled(boolean enabled) {
        for (Button button : buttons) {
            button.visible = enabled;
            button.active = enabled;
        }
    }
}