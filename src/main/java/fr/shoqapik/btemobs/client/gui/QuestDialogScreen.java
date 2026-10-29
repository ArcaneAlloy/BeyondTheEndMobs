package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.client.BteMobsModClient;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.ActionPacket;
import fr.shoqapik.btemobs.option_dialogs.OptionDialogs;
import fr.shoqapik.btemobs.option_dialogs.QuestAnswer;
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

    // ---- Colocacion: panel de dialogo + columna de respuestas, centrados en horizontal con cualquier escala ----
    // Antes colgaban de width/8, asi que se descentraban al cambiar la escala de la interfaz o la ventana.
    private static final int ANSWER_WIDTH = 100;
    /**
     * Distancia del panel al borde de abajo: por encima de la barra de items (22 px), los corazones y la comida
     * (hasta 39 px) y la armadura (hasta 49 px), para no tapar el HUD. Antes eran 25 px y lo tapaba.
     */
    private static final int DIALOG_BOTTOM_MARGIN = 52;

    /** Borde izquierdo del panel de dialogo; las respuestas van justo a su derecha. */
    private int dialogLeft() { return Math.max(2, (this.width - (this.imageWidth + ANSWER_WIDTH)) / 2); }
    private int dialogTop() { return this.height - this.imageHeight - DIALOG_BOTTOM_MARGIN; }

    private int entityId;
    private BteNpcType bteNpcType;
    private OptionDialogs quest;
    private boolean typing;
    private int letterIndex;
    private String currentLine = "";
    private int page;
    private ResourceLocation currentDialogSound;
    private boolean muteSound;

    private List<Button> buttons = new ArrayList<>();
    private boolean declined;
    /** Botón pequeño que abre la guía del NPC (su tutorial); null si el NPC no tiene guía. */
    private TutorialGuideButton tutorialButton;

    public QuestDialogScreen(int entityId, BteNpcType bteNpcType, OptionDialogs quest) {
        this(entityId, bteNpcType, quest, false);
    }

    public QuestDialogScreen(int entityId, BteNpcType bteNpcType, OptionDialogs quest, boolean muteSound) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quest = quest;
        this.muteSound = muteSound;
    }


    private String tr(String key) {
        return key != null && key.contains(".") ? I18n.get(key) : key;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int x = dialogLeft() + this.imageWidth;
        int y = dialogTop() - 15;
        // Antonio tiene 3 opciones (ya no tiene "Guide me!"): se bajan para que la última quede a la misma altura
        // que la de los NPC con 4 y no quede un hueco debajo
        if (bteNpcType == BteNpcType.EXPLORER && quest.getAnswers().size() < 4) {
            y += (4 - quest.getAnswers().size()) * 25;
        }

        // Al volver de la ventana de confirmación se llama otra vez a init: no se duplican los botones
        this.buttons.clear();
        int index = quest.getAnswers().size() > 4 ? -1 : 0;
        int firstY = y + index * 25;
        for (QuestAnswer questAnswer : this.quest.getAnswers()) {
            if (index > 3) break;

            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/background.png", bteNpcType.name().toLowerCase(Locale.ROOT)));
            ResourceLocation foregroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s.png", bteNpcType.name().toLowerCase(Locale.ROOT), questAnswer.getAction().toLowerCase(Locale.ROOT)));
            // Sin icono si la textura no existe (evita el cuadro morado y negro, p. ej. en "quests")
            if (Minecraft.getInstance().getResourceManager().getResource(foregroundTexture).isEmpty()) foregroundTexture = null;

            String answerKey = questAnswer.getFormattedAwnser();
            String translatedAnswer = answerKey.contains(".") ? I18n.get(answerKey) : answerKey;

            CustomButton button = new CustomButton(backgroundTexture, foregroundTexture, x, y + index * 25, 100, 20, Component.literal(translatedAnswer),
                    (p_95981_) -> {
                        // Opción bloqueada por una quest: no hace nada (el tooltip explica qué quest la desbloquea)
                        if (p_95981_ instanceof CustomButton cbLocked && cbLocked.isLock()) return;
                        if (questAnswer.getAction().equals("quests")){
                            Minecraft.getInstance().setScreen(new QuestScreen(this.entityId,bteNpcType, RecipeCapability.get(Minecraft.getInstance().player).getQuestForNpc(bteNpcType)));
                        }else if (questAnswer.getAction().equals("rumor")) {
                            Minecraft.getInstance().setScreen(null);
                            BteMobsModClient.handleRumorsPacket(this.entityId);
                        } else if (!questAnswer.getAction().equals("wip")) {
                            Minecraft.getInstance().setScreen(null);
                            BteMobsMod.sendToServer(new ActionPacket(entityId, questAnswer.getAction()));
                            if (BteNpcType.DRUID == this.bteNpcType) {
                                BteMobsModClient.handleClearItem(this.entityId);
                            }
                        } else {
                            // "Maybe later": durante el tutorial pregunta si quiere saltarse la guía del NPC
                            this.onClose();
                        }

                        if (currentDialogSound != null) {
                            Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
                            currentDialogSound = null;
                        }
                    }
            );
            String actionKey = bteNpcType.name().toLowerCase()+":"+questAnswer.getAction();
            RecipeCapability<?> dialogCap = RecipeCapability.get(getMinecraft().player);
            boolean locked = !dialogCap.isUnlockAction(actionKey);
            button.setIsLock(locked);
            // Tooltip con la quest que la desbloquea (se dibuja al final del frame para que ningún botón lo tape)
            button.deferTooltip = true;
            if (locked) button.listComponents = fr.shoqapik.btemobs.client.gui.QuestTexts.lockedActionTooltip(dialogCap.unlockActions, actionKey);
            buttons.add(this.addRenderableWidget(button));

            index++;
        }

        // Botón de la guía del NPC: encima de la columna de respuestas, pegado a su borde derecho
        this.tutorialButton = null;
        if (fr.shoqapik.btemobs.client.WelcomeClient.hasGuide(bteNpcType)) {
            TutorialGuideButton.resetTextureCache();
            this.tutorialButton = new TutorialGuideButton(x + 100 - TutorialGuideButton.SIZE, firstY - TutorialGuideButton.SIZE - 4, p -> {
                if (currentDialogSound != null) {
                    Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
                    currentDialogSound = null;
                }
                Minecraft.getInstance().setScreen(null);
                fr.shoqapik.btemobs.client.WelcomeClient.openGuide(bteNpcType);
            });
            buttons.add(this.addRenderableWidget(this.tutorialButton));
        }
        setButtonsEnabled(false);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        String rawDialog = this.quest.getDialogs().get(page);
        String translatedDialog = rawDialog.contains(".") ? I18n.get(rawDialog) : rawDialog;

        if (letterIndex < translatedDialog.length()) {
            if (!typing && !muteSound && page < this.quest.getDialogSounds().size()) {

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

        int x = dialogLeft();
        int y = dialogTop();

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

        // Botón de la guía: durante el tutorial, marco dorado y el aviso siempre visible; si no, tooltip al pasar el ratón
        if (tutorialButton != null && tutorialButton.visible) {
            Component name = fr.shoqapik.btemobs.client.WelcomeClient.guideName(bteNpcType);
            int bx = tutorialButton.x, by = tutorialButton.y;
            if (fr.shoqapik.btemobs.client.WelcomeClient.guidePending(bteNpcType)) {
                fr.shoqapik.btemobs.client.WelcomeClient.WelcomeForgeEvents.highlight(poseStack, partialTick, bx, by,
                        TutorialGuideButton.SIZE, TutorialGuideButton.SIZE);
                List<FormattedCharSequence> hint = font.split(Component.translatable("gui.bte_mobs.tutorial.hint", name), 150);
                int w = 0;
                for (FormattedCharSequence line : hint) w = Math.max(w, font.width(line));
                // renderTooltip lo dibuja en (x + 12, y - 12): se deja a la izquierda del botón (y de su flecha)
                this.renderTooltip(poseStack, hint, bx - 16 - w - 12, by + 12 + (TutorialGuideButton.SIZE - 8) / 2 - (hint.size() - 1) * 5);
            } else if (tutorialButton.isMouseOver(mouseX, mouseY)) {
                this.renderTooltip(poseStack, Component.translatable("gui.bte_mobs.tutorial.button", name), mouseX, mouseY);
            }
        }

        for (Button b : buttons) {
            if (b instanceof CustomButton cb && cb.isLock() && cb.visible && cb.isMouseOver(mouseX, mouseY)
                    && cb.listComponents != null && !cb.listComponents.isEmpty()) {
                this.renderComponentTooltip(poseStack, cb.listComponents, mouseX, mouseY);
            }
        }
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
            this.onClose();

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

    /**
     * Esc o "Maybe later" mientras el tutorial pide ver la guía de este NPC: en vez de cerrar, se pregunta si de
     * verdad quiere saltársela (si dice que no, vuelve a este diálogo).
     */
    @Override
    public void onClose() {
        if (fr.shoqapik.btemobs.client.WelcomeClient.guidePending(bteNpcType)) {
            if (currentDialogSound != null) {
                Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
                currentDialogSound = null;
            }
            Minecraft.getInstance().setScreen(fr.shoqapik.btemobs.client.WelcomeClient.skipGuideScreen(bteNpcType, this));
            return;
        }
        super.onClose();
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