package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.StatTaskData;
import fr.shoqapik.btemobs.capability.UnlockState;
import fr.shoqapik.btemobs.client.widget.ButtonReward;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.packets.QuestActionPacket;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QuestScreen extends Screen {

    public static final ResourceLocation DIALOGS_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/default.png");
    private static final ResourceLocation GUI_BARS_LOCATION = new ResourceLocation(BteMobsMod.MODID,"textures/gui/bars.png");

    protected int imageWidth = 254;
    protected int imageHeight = 168;
    protected int leftPos;
    protected int topPos;
    protected Button up;
    protected Button down;
    protected int scrolledY=0;
    private int entityId;
    private BteNpcType bteNpcType;
    private Map<Quest, QuestStateData> quests;
    private Quest currentQuest =null;
    private List<Button> buttons = new ArrayList<>();
    private List<ButtonReward> slotRewards= new ArrayList<>();
    private Button stateQuest;
    private boolean isComplete;
    private boolean isReclaim;
    private boolean dirty = false;
    public float[][] colors = new float[][]{
            {1.0f,0.0f,0.0f,1.0F},
            {0.0F,1.0F,0.0F,1.0F},
            {0.0F,1.0F,0.0F,1.0F}
    };
    public QuestScreen(int entityId, BteNpcType bteNpcType, Map<Quest, QuestStateData> quests) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quests = quests;

    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.clearWidgets();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - 80) / 2;

        quests = ((Map<Quest,QuestStateData>)RecipeCapability.get(minecraft.player).quests.get(bteNpcType));
        List<Map.Entry<Quest, QuestStateData>> sortedQuests = new ArrayList<>(this.quests.entrySet());
        sortedQuests.sort(Comparator.comparingInt(entry -> getQuestOrder(entry.getValue())));
        for (Map.Entry<Quest,QuestStateData> rumor : sortedQuests) {
            String id = rumor.getKey().getPriorityQuest() == Quest.PriorityQuest.MAIN_QUEST ? "background_main.png" : "background.png" ;
            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s", bteNpcType.name().toLowerCase(Locale.ROOT),id));

            boolean isUnlock = (rumor.getValue().unlockStates.isEmpty() || rumor.getValue().unlockStates.stream().allMatch(e->e.unlock));

            String rawTitle = "title.quest."+rumor.getKey().id.toString().split(":")[1];
            String translatedTitle = rawTitle.contains(".") ? I18n.get(rawTitle) : rawTitle;

            float[] color = rumor.getValue().isReclaim ? new float[]{0,1.0F,0,1.0F} : new float[]{1.0F,1.0F,1.0f,1.0f};
            CustomButton button = new CustomButton(backgroundTexture, null , 0, 0, 100, 20,color, Component.literal(translatedTitle),List.of(Component.literal(rumor.getKey().getToolTip())),
                    (p_95981_) -> {
                        if(isUnlock){
                            buttons.forEach(button1 -> ((CustomButton)button1).isSelect = false);
                            this.currentQuest = rumor.getKey();
                            refreshButton();
                            ((CustomButton)p_95981_).isSelect=true;
                        }
                    });

            button.setIsLock(!isUnlock);
            buttons.add(this.addRenderableWidget(button));
        }


        this.up = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height -80)-150,14,16,0,0,0,
                new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/up.png"),14,16,(p)->{
            scrolledY = Math.max(0,scrolledY-1);
            this.layoutButtons();
        });

        this.down = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height - 80)+45,14,16,0,0,0,
                new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/down.png"),14,16,(p)->{
            scrolledY=Math.min(this.quests.size(),this.scrolledY+1);
            this.layoutButtons();
        });
        this.stateQuest = this.addRenderableWidget(new Button((this.leftPos - (this.width / 8))+137 ,(this.height - 80),150,15,Component.literal("Quest Incomplete."),(p)->{
            refreshButton();
            if (this.isComplete && !this.isReclaim){
                BteMobsMod.sendToServer(new QuestActionPacket(currentQuest,0,minecraft.player.getId()));
                stateQuest.active = false;
                this.dirty = true;
            }
        }){
            @Override
            public void renderButton(PoseStack p_93676_, int p_93677_, int p_93678_, float p_93679_) {
                Minecraft minecraft = Minecraft.getInstance();
                Font font = minecraft.font;
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
                int index = isComplete ? 1 : 0 ;
                if (isReclaim){
                    index = 2;
                }
                float r = colors[index][0];
                float g = colors[index][1];
                float b = colors[index][2];
                float a = colors[index][3];
                RenderSystem.setShaderColor(r, g, b, a);
                int i = this.getYImage(this.isHoveredOrFocused());
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                this.blit(p_93676_, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                this.blit(p_93676_, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
                this.renderBg(p_93676_, minecraft, p_93677_, p_93678_);
                int j = getFGColor();
                drawCenteredString(p_93676_, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);
            }
        });
        this.addRenderableWidget(this.up);
        this.addRenderableWidget(this.down);
        this.layoutButtons();
        this.stateQuest.visible = false;
        if (this.currentQuest!=null){
            this.refreshButton();
        }
        BteMobsMod.sendToServer(new QuestActionPacket(currentQuest,1,minecraft.player.getId()));

    }

    @Override
    public void renderComponentTooltip(PoseStack poseStack, List<? extends FormattedText> tooltips, int mouseX, int mouseY, @Nullable Font font) {
        super.renderComponentTooltip(poseStack,List.of(Component.literal("Lore")), mouseX, mouseY, font);
    }

    public void refreshButton(){
        slotRewards.clear();
        int i = 0;
        int x = (int) (this.leftPos - (this.width / 8)  +90);
        int y = (int) (this.height - 80 - 130);
        for (RewardData data : this.currentQuest.getRewards()){
            ButtonReward buttonReward = new ButtonReward(x+12 + 18 * i ,y+39,16,16,data,minecraft);
            slotRewards.add(buttonReward);
            i++;
        }
        if (currentQuest!=null && minecraft.player!=null){
            isComplete = RecipeCapability.get(minecraft.player).getQuestComplete(currentQuest);
            isReclaim = RecipeCapability.get(minecraft.player).getQuestReclaim(currentQuest);
            stateQuest.visible = true;
            stateQuest.active = !isReclaim;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.player == null || currentQuest == null)
            return;

        QuestStateData state = ((Map<Quest, QuestStateData>) RecipeCapability.get(minecraft.player).quests.get(bteNpcType)).get(currentQuest);

        if (state == null)
            return;

        boolean newComplete = state.isComplete;
        boolean newReclaim = state.isReclaim;

        if (newComplete != this.isComplete || newReclaim != this.isReclaim) {

            this.isComplete = newComplete;
            this.isReclaim = newReclaim;

            refreshButton();

            this.quests = (Map<Quest, QuestStateData>) RecipeCapability.get(minecraft.player).quests.get(bteNpcType);

            List<Map.Entry<Quest, QuestStateData>> sortedQuests = new ArrayList<>(this.quests.entrySet());
            sortedQuests.sort(Comparator.comparingInt(entry -> getQuestOrder(entry.getValue())));

            buttons.forEach(this::removeWidget);
            buttons.clear();

            for (Map.Entry<Quest, QuestStateData> entry : sortedQuests) {
                String id = entry.getKey().getPriorityQuest() == Quest.PriorityQuest.MAIN_QUEST ? "background_main.png" : "background.png" ;
                ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s", bteNpcType.name().toLowerCase(Locale.ROOT),id));

                boolean isUnlock = (entry.getValue().unlockStates.isEmpty() || entry.getValue().unlockStates.stream().allMatch(e->e.unlock));

                String rawTitle = "title.quest."+entry.getKey().id.toString().split(":")[1];
                String translatedTitle = rawTitle.contains(".") ? I18n.get(rawTitle) : rawTitle;

                float[] color = entry.getValue().isReclaim ? new float[]{0,1.0F,0,1.0F} : new float[]{1.0F,1.0F,1.0f,1.0f};
                CustomButton button = new CustomButton(backgroundTexture, null , 0, 0, 100, 20,color, Component.literal(translatedTitle),List.of(Component.literal(entry.getKey().getToolTip())),
                        (p_95981_) -> {
                            if(isUnlock){
                                buttons.forEach(button1 -> ((CustomButton)button1).isSelect = false);
                                this.currentQuest = entry.getKey();
                                refreshButton();
                                ((CustomButton)p_95981_).isSelect=true;
                            }
                        });

                button.setIsLock(!isUnlock);
                if (entry.getValue().isComplete && !entry.getValue().isReclaim){
                    button.isSelect = true;
                }
                buttons.add(this.addRenderableWidget(button));
            }

            layoutButtons();
        }
    }

    private int getQuestOrder(QuestStateData state) {
        boolean unlock = state.unlockStates.stream().allMatch((unlockData)->unlockData.unlock);

        if (!state.unlockStates.isEmpty() && !unlock){
            return 1;
        }
        if (state.isComplete && !state.isReclaim) {
            return -1; // completada, recompensa pendiente
        }

        if (!state.isComplete) {
            return 0; // todavía no completada
        }
        return 2; // completada y recompensa obtenida
    }
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if(this.currentQuest !=null){
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID, "textures/gui/dialogs/antonio_tdialogo_extendido.png"));

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            int x = (int) (this.leftPos - (this.width / 8)  +90);
            int y = (int) (this.height - 80 - 130);

            GuiComponent.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

            String rawDesc = this.currentQuest.getDescription();
            String translatedDesc = rawDesc.contains(".") ? I18n.get(rawDesc) : rawDesc;
            String difficulty = currentQuest.getDifficult().name();
            int textWidth = this.font.width(difficulty);
            int left = x + 70 ;
            int top = y + 12 ;
            int right = left + textWidth + 10 ;
            int bottom = top + this.font.lineHeight + 2;
            drawWordWrap(Component.literal(translatedDesc),x + 12 , y + 100 , 197, 16777215, font, poseStack);
            poseStack.pushPose();
            poseStack.translate(42,0,0);
            fill(poseStack, left, top, right, bottom, currentQuest.getDifficult().color);
            drawWordWrap(Component.literal(currentQuest.getDifficult().name()),x + 76 , y + 15 , 100, 16777215, font, poseStack);
            poseStack.popPose();

            poseStack.pushPose();
            right = left + this.font.width(currentQuest.getDimension().name()) + 10;
            poseStack.translate(90,0,0);
            fill(poseStack, left, top, right, bottom, 0x4FFF0000);
            drawWordWrap(Component.literal(currentQuest.getDimension().name()),x + 76 , y + 15 , 220, 16777215, font, poseStack);
            poseStack.popPose();
            poseStack.pushPose();

            int left1 = x + 70;
            int top1 = y + 12;
            int right1 = left;
            int bottom1 = top + this.font.lineHeight + 2;
            fill(poseStack, left1, top1, right1, bottom1, 0x4FFF0000);
            poseStack.popPose();
            poseStack.pushPose();
            drawWordWrap(Component.literal("Task :"), x + 112, y + 30, 220, 16777215, font, poseStack);
            int i = 0;
            int j = 0;
            Component prevComponent = null;
            for (StatTaskData data : quests.get(currentQuest).statTaskData){
                poseStack.pushPose();
                int d = 0;
                if (prevComponent !=null){
                    d = font.width(prevComponent);
                }
                poseStack.translate(x + 12 + d*j  +101,y + i * 6 + 38,0.0D);
                poseStack.scale(0.5F,0.5F,0.5F);
                Component component = getComponentForType(data);
                drawWordWrap(component,0 ,0 , 150, 16777215, font, poseStack);
                bar(poseStack,data);
                i++;
                if (i == 3 ){
                    j++;
                    i = 0;
                }
                prevComponent = component;
                poseStack.popPose();
            }

            poseStack.popPose();

            drawWordWrap(Component.literal("Rewards :"), x + 12, y + 30, 220, 16777215, font, poseStack);

            poseStack.popPose();

            renderButton(poseStack, mouseX, mouseY, partialTick);

        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
    public void bar(PoseStack poseStack,StatTaskData statTaskData){
        poseStack.pushPose();
        int i = this.minecraft.getWindow().getGuiScaledWidth();
        int j = 12;
        int k =i/2 - 91;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_BARS_LOCATION);
        poseStack.translate(-50.0,-12.0,0);
        this.drawBar(poseStack, k, j, (float) statTaskData.count/(float) statTaskData.maxCount);
        Component component = Component.empty();
        int l = this.minecraft.font.width(component);
        int i1 = i/2 - l / 2;
        int j1 = j - 9;
        this.minecraft.font.drawShadow(poseStack, component, (float)i1, (float)j1, 16777215);

        if (j >= this.minecraft.getWindow().getGuiScaledHeight() / 3) {

        }
        poseStack.popPose();
    }
    private void drawBar(PoseStack p_93707_, int p_93708_, int p_93709_, float porcent) {
        this.drawBar(p_93707_, p_93708_, p_93709_, porcent, 115, 0);
        int i = (int)(porcent * 115.0F);
        if (i > 0) {
            this.drawBar(p_93707_, p_93708_, p_93709_, porcent, i, 5);
        }

    }

    private void drawBar(PoseStack p_232470_, int p_232471_, int p_232472_, float porcent, int p_232474_, int p_232475_) {
        this.blit(p_232470_, p_232471_, p_232472_, 0,  + p_232475_, p_232474_, 5);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.blit(p_232470_, p_232471_, p_232472_, 0, 80  + p_232475_, p_232474_, 5);
        RenderSystem.disableBlend();

    }
    public Component getComponentForType(StatTaskData data){
        String name = "";
        switch (data.taskType){
            case HUNTER -> {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(data.id));
                name= type.getDescription().getString();
            }
            case COLLECT -> {
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.id)));
                name= stack.getHoverName().getString();
            }
        }
        return Component.literal(name+"  " + data.count +"/"+data.maxCount);
    }
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick){
        if (!this.currentQuest.getRewards().isEmpty()){
            for (ButtonReward buttonReward : slotRewards){
                buttonReward.render(poseStack,mouseX,mouseY,partialTick);
            }
        }
    }

    @Override
    protected void renderTooltip(PoseStack p_96566_, ItemStack p_96567_, int p_96568_, int p_96569_) {
        super.renderTooltip(p_96566_, p_96567_, p_96568_, p_96569_);
    }

    @Override
    public boolean isMouseOver(double p_96595_, double p_96596_) {
        return super.isMouseOver(p_96595_, p_96596_);
    }

    private void layoutButtons() {
        int x = (int) (this.leftPos - (this.width / 8) - 22);
        int yStart = (int) (this.height - 80 - 130);

        int visibleStartIndex = scrolledY;
        int visibleEndIndex = Math.min(scrolledY + 7, quests.size());

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i >= visibleStartIndex && i < visibleEndIndex) {
                int visualIndex = i - visibleStartIndex;
                button.visible = true;
                button.active = true;
                button.y=yStart + visualIndex * 25;
                button.x=x;
            } else {
                button.visible = false;
                button.active = false;
            }
        }

        this.up.visible = visibleStartIndex > 0;
        this.up.active = visibleStartIndex > 0;

        this.down.visible = visibleEndIndex < quests.size();
        this.down.active = visibleEndIndex < quests.size();
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
        return super.mouseClicked(p_94695_, p_94696_, p_94697_);
    }


    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (this.quests.size()>7) {
            int j = this.quests.size() - 7;
            this.scrolledY = Mth.clamp((int)((double)this.scrolledY - pDelta), 0, j);
            this.layoutButtons();
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
