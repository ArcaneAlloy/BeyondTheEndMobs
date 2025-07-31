package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.ActionPacket;
import fr.shoqapik.btemobs.quests.Rumor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RumorsScreen extends Screen {

    public static final ResourceLocation DIALOGS_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/default.png");
    private static final Logger log = LoggerFactory.getLogger(RumorsScreen.class);
    protected int imageWidth = 254;
    protected int imageHeight = 80;
    protected int leftPos;
    protected int topPos;
    protected Button up;
    protected Button down;
    protected int scrolledY=0;
    private int entityId;
    private BteNpcType bteNpcType;
    private List<Rumor> rumors;
    private Rumor currentRumor=null;

    private List<Button> buttons = new ArrayList<>();

    public RumorsScreen(int entityId, BteNpcType bteNpcType, List<Rumor> rumors) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.rumors = rumors;
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.clearWidgets();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int index = 0;
        for (Rumor rumor : this.rumors) {

            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/background.png", bteNpcType.name().toLowerCase(Locale.ROOT)));

            boolean isUnlock = rumor.getUnlockLevel().isUnlocked(BteMobsMod.unlockLevel);
            CustomButton button = new CustomButton(
                    backgroundTexture, null ,
                    0,
                    0,
                    100,
                    20,
                    Component.literal(rumor.getTitle()),
                    (p_95981_) -> {
                        if(isUnlock){
                            this.currentRumor=rumor;
                        }
                    }
            );
            button.setIsLock(!isUnlock);
            buttons.add(this.addRenderableWidget(button));

            index++;
        }

        this.up = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height - this.imageHeight)-150,14,16,0,0,0,new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/up.png"),14,16,(p)->{
            scrolledY = Math.max(0,scrolledY-1);
            this.layoutButtons();
        });

        this.down = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height - this.imageHeight)+45,14,16,0,0,0,new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/down.png"),14,16,(p)->{
            scrolledY=Math.min(this.rumors.size(),this.scrolledY+1);
            this.layoutButtons();
        });
        this.addRenderableWidget(this.up);
        this.addRenderableWidget(this.down);
        this.layoutButtons();
        super.init();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        if(this.currentRumor!=null){
            // Render background
            RenderSystem.setShader(GameRenderer::getPositionTexShader);

            RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/dialogs/%s.png", bteNpcType.name().toLowerCase(Locale.ROOT))));

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            int x = (int) (this.leftPos - (this.width / 8) + 90);
            int y = (int) (this.height - this.imageHeight - 130);

            GuiComponent.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

            //GuiComponent.drawCenteredString(poseStack, font, "Rumors", x + imageWidth / 2, y + 5, 16777215);

            drawWordWrap(Component.literal(this.currentRumor.getDescription()), x + 17, y + 30, 240 - 20, 16777215, font, poseStack);


            poseStack.popPose();
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void layoutButtons() {
        int x = (int) (this.leftPos - (this.width / 8) - 22);
        int yStart = (int) (this.height - this.imageHeight - 130);

        int visibleStartIndex = scrolledY;
        int visibleEndIndex = Math.min(scrolledY + 7, rumors.size());

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i >= visibleStartIndex && i < visibleEndIndex) {
                int visualIndex = i - visibleStartIndex;
                button.visible = true;
                button.active = true;
                button.y=yStart + visualIndex * 25; // apply vertical scroll
                button.x=x;
            } else {
                button.visible = false; // hide buttons outside of scroll window
                button.active = false;
            }
        }
        if(visibleStartIndex>0){
            this.up.active = true;
            this.up.visible = true;
        }else {
            this.up.active = false;
            this.up.visible = false;
        }

        if(visibleEndIndex<rumors.size()){
            this.down.visible=true;
            this.down.active=true;
        }else {
            this.down.visible=false;
            this.down.active=false;
        }
    }

    public void drawWordWrap(FormattedText p_92858_, int p_92859_, int p_92860_, int p_92861_, int p_92862_, Font font, PoseStack stack) {
        Matrix4f matrix4f = stack.last().pose();

        for (FormattedCharSequence formattedcharsequence : font.split(p_92858_, p_92861_)) {
            font.drawInternal(formattedcharsequence, (float) p_92859_, (float) p_92860_, p_92862_, matrix4f, false);
            p_92860_ += 11;
        }

    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (this.rumors.size()>7) {
            int j = this.rumors.size() - 7;
            this.scrolledY = Mth.clamp((int)((double)this.scrolledY - pDelta), 0, j);
            this.layoutButtons();
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
        return super.mouseClicked(p_94695_, p_94696_, p_94697_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
