package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.shadowed.eliotlash.mclib.math.functions.limit.Min;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class QuestDialogScreen extends Screen {

    public static final ResourceLocation DIALOGS_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/default.png");
    private static final Logger log = LoggerFactory.getLogger(QuestDialogScreen.class);
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

/*    private Button acceptQuestButton;
    private Button declineQuestButton;
  */

    public QuestDialogScreen(int entityId, BteNpcType bteNpcType, Quest quest) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quest = quest;
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

            buttons.add(this.addRenderableWidget(new CustomButton(
                    backgroundTexture, foregroundTexture,
                    x,
                    y + index * 25,
                    100,
                    20,
                    Component.literal(questAnswer.getFormattedAwnser()),
                    (p_95981_) -> {
                        if (!questAnswer.getAction().equals("wip")) {
                            Minecraft.getInstance().setScreen(null);
                            BteMobsMod.sendToServer(new ActionPacket(entityId, questAnswer.getAction()));
                        } else {
                            this.quest.getDialogs().add("Not implemented yet! Come back later.");
                            this.page = page + 1;
                            this.declined = true;
                            setButtonsEnabled(false);
                            letterIndex = 0;
                            currentLine = "";
                        }
                    }
            )));

            index++;
        }
        setButtonsEnabled(false);

        super.init();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (letterIndex < this.quest.getDialogs().get(page).length()) {
            if(!typing && page < this.quest.getDialogSounds().size()) {
                ResourceLocation soundLocation = this.quest.getDialogSounds().get(page);
                double x = Minecraft.getInstance().player.getX();
                double y = Minecraft.getInstance().player.getY();
                double z = Minecraft.getInstance().player.getZ();
                this.minecraft.getSoundManager().play(new SimpleSoundInstance(soundLocation, SoundSource.NEUTRAL, 1.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false));
                this.currentDialogSound = soundLocation;
            }

            typing = true;
            currentLine += this.quest.getDialogs().get(page).charAt(letterIndex);
            letterIndex += 1;
        } else {
            typing = false;
        }
        this.setFocused(null);

        // Render background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/dialogs/%s.png", bteNpcType.name().toLowerCase(Locale.ROOT))));

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int x = this.leftPos - (this.width / 8);
        int y = this.height - this.imageHeight - 25;

        GuiComponent.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

        //GuiComponent.drawCenteredString(poseStack, font, entityname, x + imageWidth / 2, y + 5, 16777215);

        drawWordWrap(Component.literal(currentLine), x + 17, y + 30, 240 - 20, 16777215, font, poseStack);


        poseStack.popPose();
        if (!typing) {
            if (page != this.quest.getDialogs().size() - 1 || declined) {
                GuiComponent.drawCenteredString(poseStack, font, "Click anywhere to continue", x + imageWidth / 2, y - 13, 16777215);
            } else {
                setButtonsEnabled(true);
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    public void drawWordWrap(FormattedText p_92858_, int p_92859_, int p_92860_, int p_92861_, int p_92862_, Font font, PoseStack stack) {
        Matrix4f matrix4f = stack.last().pose();

        for (FormattedCharSequence formattedcharsequence : font.split(p_92858_, p_92861_)) {
            font.drawInternal(formattedcharsequence, (float) p_92859_, (float) p_92860_, p_92862_, matrix4f, false);
            p_92860_ += 11;
        }

    }

    @Override
    public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
        if (typing) {
            typing = false;
            currentLine = this.quest.getDialogs().get(page);
            letterIndex = currentLine.length();
            if(currentDialogSound != null) {
                Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
            }
        } else if (page != this.quest.getDialogs().size() - 1) {
            page += 1;
            letterIndex = 0;
            currentLine = "";
            if(currentDialogSound != null) {
                Minecraft.getInstance().getSoundManager().stop(currentDialogSound, SoundSource.NEUTRAL);
            }
        } else if (declined) {
            Minecraft.getInstance().setScreen(null);
        } else if (page == this.quest.getDialogs().size() - 1) {
            return super.mouseClicked(p_94695_, p_94696_, p_94697_);
        }
        return true;
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
