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
import fr.shoqapik.btemobs.client.widget.ButtonTask;
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
    // Desplazamiento vertical de la cabecera (Easy/Overworld, Rewards, Task, items y barras). Positivo = más abajo.
    private static final int HEADER_OFFSET_Y = 0;
    // Columnas Rewards y Task: la etiqueta de cada una se centra en su mitad del panel
    // y los iconos se alinean a la izquierda con el borde izquierdo de la etiqueta.
    // Estos valores mueven la columna entera (etiqueta + iconos + barras). Positivo = más a la derecha.
    private static final int REWARDS_OFFSET_X = 0;
    private static final int TASK_OFFSET_X = -6;
    // Medidas de los elementos de las columnas
    private static final int LABEL_WIDTH = 73;       // ancho de label_rewards.png / label_task.png
    private static final int SLOT_SIZE = 20;         // tamaño del botón de cada item (el item de 16x16 va centrado dentro)
    private static final int REWARD_SPACING = 22;    // distancia entre recompensas
    private static final int TASK_ROW_SPACING = 21;  // distancia entre filas de tareas
    // Separación vertical extra entre las etiquetas Rewards/Task y sus iconos. Positivo = iconos más abajo.
    private static final int REWARDS_ITEMS_GAP_Y = 4;
    private static final int TASK_ITEMS_GAP_Y = 4;
    private static final int TASK_BAR_GAP = 4;       // hueco entre icono y barra
    private static final int TASK_BAR_WIDTH = 58;    // ancho de la barra (115 px de textura a escala 0.5)
    // Desplazamiento propio de las etiquetas Easy/Overworld (independiente de HEADER_OFFSET_Y y de las columnas).
    private static final int LABELS_OFFSET_X = 10;
    private static final int LABELS_OFFSET_Y = 0;
    // Desplazamiento vertical del botón Quest Incomplete/Complete. Positivo = más abajo.
    private static final int QUEST_BUTTON_OFFSET_Y = 8;
    // Desplazamiento horizontal de la descripción de la quest. Positivo = más a la derecha.
    private static final int DESCRIPTION_OFFSET_X = 2;
    // Desplazamiento vertical de la descripción. Negativo = más arriba.
    private static final int DESCRIPTION_OFFSET_Y = -8;
    // Margen entre el final del texto de la descripción y el borde derecho del panel.
    // Menos margen = líneas más largas.
    private static final int DESCRIPTION_RIGHT_MARGIN = 18;
    protected int leftPos;
    private int rewardsCenterX(int panelX) { return panelX + imageWidth / 4 + REWARDS_OFFSET_X; }
    private int taskCenterX(int panelX) { return panelX + imageWidth * 3 / 4 + TASK_OFFSET_X; }
    private int rewardsLeftX(int panelX) { return rewardsCenterX(panelX) - LABEL_WIDTH / 2; }
    private int taskLeftX(int panelX) { return taskCenterX(panelX) - LABEL_WIDTH / 2; }
    protected int topPos;
    protected Button up;
    protected Button down;
    protected int scrolledY=0;
    private int entityId;
    private BteNpcType bteNpcType;
    /** Abierta desde el inventario (QuestLogScreen): solo consulta, no se puede reclamar. */
    private boolean readOnly = false;
    @Nullable
    private net.minecraft.client.gui.screens.Screen parent;
    private Map<Quest, QuestStateData> quests;
    private Quest currentQuest =null;
    private List<Button> buttons = new ArrayList<>();
    private List<ButtonReward> slotRewards= new ArrayList<>();
    private List<ButtonTask> slotTask= new ArrayList<>();

    private Button stateQuest;
    /** Casilla "Seguir": muestra la quest en el panel de seguimiento (QuestTracker). */
    private Button trackBox;
    private static final ResourceLocation CHECKBOX = new ResourceLocation("textures/gui/checkbox.png");
    private boolean isComplete;
    private boolean isReclaim;
    private boolean dirty = false;
    public float[][] colors = new float[][]{
            {1.0f,0.0f,0.0f,1.0F},
            {0.0F,1.0F,0.0F,1.0F},   // lista para reclamar
            {0.6F,0.6F,0.6F,1.0F}    // ya reclamada
    };

    /** Texto del botón de estado según la quest seleccionada (claves en los archivos lang). */
    private Component questStateMessage() {
        if (this.isReclaim) return Component.translatable("gui.bte_mobs.quest.completed");
        if (this.readOnly && this.isComplete) return Component.translatable("gui.bte_mobs.quest.claim_at",
                Component.translatable("entity.bte_mobs." + bteNpcType.name().toLowerCase(Locale.ROOT)));
        if (this.isComplete) return Component.translatable("gui.bte_mobs.quest.claim");
        return Component.translatable("gui.bte_mobs.quest.incomplete");
    }
    public QuestScreen(int entityId, BteNpcType bteNpcType, Map<Quest, QuestStateData> quests) {
        super(Component.literal(bteNpcType.name().toLowerCase(Locale.ROOT)));
        this.entityId = entityId;
        this.bteNpcType = bteNpcType;
        this.quests = quests;

    }

    /** Solo consulta (botón de quests del inventario): mismas quests, pero sin poder reclamarlas. */
    public QuestScreen(BteNpcType bteNpcType, @Nullable net.minecraft.client.gui.screens.Screen parent) {
        this(-1, bteNpcType, new java.util.HashMap<>());
        this.readOnly = true;
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.clearWidgets();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - 80) / 2;

        quests = ((Map<Quest,QuestStateData>)RecipeCapability.get(minecraft.player).quests.get(bteNpcType));
        if (quests == null) quests = new java.util.HashMap<>(); // aún no ha llegado la sincronización del servidor
        List<Map.Entry<Quest, QuestStateData>> sortedQuests = new ArrayList<>(this.quests.entrySet());
        sortedQuests.sort(QUEST_ORDER);
        for (Map.Entry<Quest,QuestStateData> rumor : sortedQuests) {
            String id = rumor.getKey().getPriorityQuest() == Quest.PriorityQuest.MAIN_QUEST ? "background_main.png" : "background.png" ;
            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s", bteNpcType.name().toLowerCase(Locale.ROOT),id));

            boolean isUnlock = (rumor.getValue().unlockStates.isEmpty() || rumor.getValue().unlockStates.stream().allMatch(e->e.unlock));

            String translatedTitle = QuestTexts.title(rumor.getKey());

            float[] color = rumor.getValue().isReclaim ? new float[]{0,1.0F,0,1.0F} : new float[]{1.0F,1.0F,1.0f,1.0f};
            CustomButton button = new CustomButton(backgroundTexture, null , 0, 0, 100, 20,color, Component.literal(translatedTitle),List.of(Component.literal(QuestTexts.tooltip(rumor.getKey()))),
                    (p_95981_) -> {
                        if(isUnlock){
                            buttons.forEach(button1 -> ((CustomButton)button1).isSelect = false);
                            this.currentQuest = rumor.getKey();
                            refreshButton();
                            ((CustomButton)p_95981_).isSelect=true;
                        }
                    });

            button.setIsLock(!isUnlock);
            button.deferTooltip = true;
            if (!isUnlock) button.listComponents = QuestTexts.lockedTooltip(rumor.getValue());
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
        this.stateQuest = this.addRenderableWidget(new Button((this.leftPos - (this.width / 8))+137 ,(this.height - 80) + QUEST_BUTTON_OFFSET_Y,150,20,Component.translatable("gui.bte_mobs.quest.incomplete"),(p)->{
            refreshButton();
            if (!this.readOnly && this.isComplete && !this.isReclaim){
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
                // El texto se actualiza en cada frame para que siempre coincida con el estado
                this.setMessage(questStateMessage());
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
        this.trackBox = this.addRenderableWidget(new Button(this.stateQuest.x + this.stateQuest.getWidth() + 6, this.stateQuest.y, 20, 20,
                Component.translatable("gui.bte_mobs.tracker.track"), b -> {
            if (currentQuest == null) return;
            fr.shoqapik.btemobs.client.QuestTracker.ToggleResult result = fr.shoqapik.btemobs.client.QuestTracker.toggle(currentQuest);
            if (result == fr.shoqapik.btemobs.client.QuestTracker.ToggleResult.LIMIT && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gui.bte_mobs.tracker.limit",
                        fr.shoqapik.btemobs.client.QuestTracker.max()).withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }, (button, poseStack, mouseX, mouseY) -> renderTooltip(poseStack,
                Component.translatable("gui.bte_mobs.tracker.track_tooltip",
                        fr.shoqapik.btemobs.client.QuestTracker.count(), fr.shoqapik.btemobs.client.QuestTracker.max()), mouseX, mouseY)) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, CHECKBOX);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                boolean tracked = fr.shoqapik.btemobs.client.QuestTracker.isTracked(currentQuest);
                // checkbox.png: u = resaltado (ratón encima), v = marcado
                GuiComponent.blit(poseStack, this.x, this.y, this.isHovered ? 20.0F : 0.0F, tracked ? 20.0F : 0.0F, 20, 20, 64, 64);
                drawString(poseStack, font, this.getMessage(), this.x + 24, this.y + (this.height - 8) / 2, 0xE0E0E0);
                if (this.isHovered) this.renderToolTip(poseStack, mouseX, mouseY);
            }
        });
        this.addRenderableWidget(this.up);
        this.addRenderableWidget(this.down);
        this.layoutButtons();
        this.stateQuest.visible = false;
        this.trackBox.visible = false;
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
        slotTask.clear();
        int i = 0;
        int x = (int) (this.leftPos - (this.width / 8)  +90);
        int y = (int) (this.height - 80 - 130);
        for (StatTaskData data : this.quests.get(this.currentQuest).statTaskData){
            ButtonTask buttonReward = new ButtonTask(taskLeftX(x), y+39 + HEADER_OFFSET_Y + TASK_ITEMS_GAP_Y + TASK_ROW_SPACING * i, SLOT_SIZE, SLOT_SIZE, data, minecraft);
            buttonReward.displayDescription = QuestTexts.task(this.currentQuest, i, data.description);
            slotTask.add(buttonReward);
            i++;
        }
        i = 0;
        int rewardsStartX = rewardsLeftX(x);
        for (RewardData data : this.currentQuest.getRewards()){
            ButtonReward buttonReward = new ButtonReward(rewardsStartX + REWARD_SPACING * i, y+39 + HEADER_OFFSET_Y + REWARDS_ITEMS_GAP_Y, SLOT_SIZE, SLOT_SIZE, data, minecraft);
            slotRewards.add(buttonReward);
            i++;
        }
        if (currentQuest!=null && minecraft.player!=null){
            isComplete = RecipeCapability.get(minecraft.player).getQuestComplete(currentQuest);
            isReclaim = RecipeCapability.get(minecraft.player).getQuestReclaim(currentQuest);
            stateQuest.visible = true;
            stateQuest.active = !readOnly && !isReclaim;
            trackBox.visible = !isReclaim;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.player == null || currentQuest == null)
            return;

        Map<Quest, QuestStateData> npcQuests = (Map<Quest, QuestStateData>) RecipeCapability.get(minecraft.player).quests.get(bteNpcType);
        if (npcQuests == null) return;
        QuestStateData state = npcQuests.get(currentQuest);

        if (state == null)
            return;

        boolean newComplete = state.isComplete;
        boolean newReclaim = state.isReclaim;

        if (newComplete != this.isComplete || newReclaim != this.isReclaim) {

            this.isComplete = newComplete;
            this.isReclaim = newReclaim;

            refreshButton();

            this.quests = npcQuests;

            List<Map.Entry<Quest, QuestStateData>> sortedQuests = new ArrayList<>(this.quests.entrySet());
            sortedQuests.sort(QUEST_ORDER);

            buttons.forEach(this::removeWidget);
            buttons.clear();

            for (Map.Entry<Quest, QuestStateData> entry : sortedQuests) {
                String id = entry.getKey().getPriorityQuest() == Quest.PriorityQuest.MAIN_QUEST ? "background_main.png" : "background.png" ;
                ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/%s/%s", bteNpcType.name().toLowerCase(Locale.ROOT),id));

                boolean isUnlock = (entry.getValue().unlockStates.isEmpty() || entry.getValue().unlockStates.stream().allMatch(e->e.unlock));

                String translatedTitle = QuestTexts.title(entry.getKey());

                float[] color = entry.getValue().isReclaim ? new float[]{0,1.0F,0,1.0F} : new float[]{1.0F,1.0F,1.0f,1.0f};
                CustomButton button = new CustomButton(backgroundTexture, null , 0, 0, 100, 20,color, Component.literal(translatedTitle),List.of(Component.literal(QuestTexts.tooltip(entry.getKey()))),
                        (p_95981_) -> {
                            if(isUnlock){
                                buttons.forEach(button1 -> ((CustomButton)button1).isSelect = false);
                                this.currentQuest = entry.getKey();
                                refreshButton();
                                ((CustomButton)p_95981_).isSelect=true;
                            }
                        });

                button.setIsLock(!isUnlock);
                button.deferTooltip = true;
                if (!isUnlock) button.listComponents = QuestTexts.lockedTooltip(entry.getValue());
                if (entry.getValue().isComplete && !entry.getValue().isReclaim){
                    button.isSelect = true;
                }
                buttons.add(this.addRenderableWidget(button));
            }

            layoutButtons();
        }
    }

    /**
     * Orden de la lista: primero por estado (para reclamar, disponibles, bloqueadas, completadas) y, dentro de cada
     * estado, por el campo "order" del JSON de la quest (progresión). A igualdad, por título.
     */
    private final Comparator<Map.Entry<Quest, QuestStateData>> QUEST_ORDER =
            Comparator.<Map.Entry<Quest, QuestStateData>>comparingInt(entry -> getQuestOrder(entry.getValue()))
                    .thenComparingInt(entry -> entry.getKey().getOrder())
                    .thenComparing(entry -> QuestTexts.title(entry.getKey()));

    private int getQuestOrder(QuestStateData state) {
        boolean unlock = state.unlockStates.stream().allMatch((unlockData)->unlockData.unlock);

        if (!state.unlockStates.isEmpty() && !unlock){
            return 1;
        }
        if (state.isComplete && !state.isReclaim) {
            return -1;
        }

        if (!state.isComplete) {
            return 0;
        }
        return 2;
    }
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.readOnly) {
            Component header = Component.translatable("gui.bte_mobs.quest_log.npc_title",
                    Component.translatable("entity.bte_mobs." + bteNpcType.name().toLowerCase(Locale.ROOT)));
            drawCenteredString(poseStack, this.font, header, this.width / 2, 8, 0xFFD700);
        }
        if(this.currentQuest !=null){
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID, "textures/gui/dialogs/"+this.currentQuest.getEntityType().name().toLowerCase()+"_tdialogo_extendido.png"));

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            int x = (int) (this.leftPos - (this.width / 8)  +90);
            int y = (int) (this.height - 80 - 130);

            GuiComponent.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

            String translatedDesc = QuestTexts.description(this.currentQuest);
            String difficulty = currentQuest.getDifficult().name();
            int textWidth = this.font.width(difficulty);
            int left = x + 70 ;
            int top = y + 12 ;
            int right = left + textWidth + 10 ;
            int bottom = top + this.font.lineHeight + 2;
            int descLeft = 12 + DESCRIPTION_OFFSET_X;
            drawWordWrap(Component.literal(translatedDesc),x + descLeft , y + 100 + DESCRIPTION_OFFSET_Y , imageWidth - descLeft - DESCRIPTION_RIGHT_MARGIN, 16777215, font, poseStack);
            poseStack.pushPose();
            renderLabel(poseStack, (int) (x + 112 -20 + LABELS_OFFSET_X), (int) (y + 11 + LABELS_OFFSET_Y), new ResourceLocation(BteMobsMod.MODID, "textures/gui/widget/label_dif_"+difficulty.toLowerCase()+".png"));
            poseStack.popPose();

            poseStack.pushPose();
            renderLabel(poseStack, (int) (x + 174 -20 + LABELS_OFFSET_X), (int) (y + 11 + LABELS_OFFSET_Y),new ResourceLocation(BteMobsMod.MODID,"textures/gui/widget/label_world_"+this.currentQuest.getDimension().name().toLowerCase()+".png"));
            poseStack.popPose();
            poseStack.pushPose();

            int left1 = x + 70;
            int top1 = y + 12;
            int right1 = left;
            int bottom1 = top + this.font.lineHeight + 2;
            fill(poseStack, left1, top1, right1, bottom1, 0x4FFF0000);
            poseStack.popPose();
            poseStack.pushPose();
            renderLabel(poseStack, taskLeftX(x), y + 26 + HEADER_OFFSET_Y, new ResourceLocation(BteMobsMod.MODID, "textures/gui/widget/label_task.png"));
            int barX = taskLeftX(x) + SLOT_SIZE + TASK_BAR_GAP;
            for (ButtonTask buttonTask : slotTask){
                int rowY = buttonTask.y;
                // Contador (0/15) centrado encima de la barra, a escala 0.5
                String count = buttonTask.data.count + "/" + buttonTask.data.maxCount;
                poseStack.pushPose();
                poseStack.translate(barX + TASK_BAR_WIDTH / 2.0F - this.font.width(count) / 4.0F, rowY + (SLOT_SIZE - 16) / 2 + 3, 0.0D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                this.font.draw(poseStack, count, 0, 0, 16777215);
                poseStack.popPose();
                // Barra de progreso debajo del contador
                bar(poseStack, buttonTask.data, barX, rowY + (SLOT_SIZE - 16) / 2 + 9);
                buttonTask.render(poseStack,mouseX,mouseY,partialTick);
            }

            poseStack.popPose();
            renderLabel(poseStack, rewardsLeftX(x), y + 26 + HEADER_OFFSET_Y, new ResourceLocation(BteMobsMod.MODID, "textures/gui/widget/label_rewards.png"));
            poseStack.popPose();

            renderButton(poseStack, mouseX, mouseY, partialTick);

        }
        super.render(poseStack, mouseX, mouseY, partialTick);
        // Tooltip de las quests bloqueadas de la lista (requisitos para desbloquearlas)
        for (Button b : buttons) {
            if (b instanceof CustomButton cb && cb.visible && cb.isLock() && cb.isMouseOver(mouseX, mouseY)
                    && cb.listComponents != null && !cb.listComponents.isEmpty()) {
                this.renderComponentTooltip(poseStack, cb.listComponents, mouseX, mouseY);
                return;
            }
        }
        // Panel de recetas fijado (si está abierto) y tooltips: al final, para que queden por encima de todo
        if (this.currentQuest != null) {
            net.minecraft.client.gui.components.AbstractWidget pinned = getPinnedAnchor();
            if (pinned != null) {
                RecipePanel panel = layoutRecipePanel(pinned);
                renderRecipePanel(poseStack, panel, mouseX, mouseY);
                int row = panel.rowAt(mouseX, mouseY);
                if (row >= 0) {
                    // A la derecha del panel, a la altura de la fila. Sin desplazar la profundidad:
                    // algunos mods dibujan iconos en el tooltip (armadura, etc.) a una z fija y quedarían tapados.
                    int tipX = panel.x + panel.width() - 8;   // renderTooltip suma +12 a la X
                    int tipY = panel.rowY(row) + 12;           // y resta 12 a la Y
                    ItemStack rowStack = panel.items.get(row);
                    java.util.List<Component> lines = new java.util.ArrayList<>(this.getTooltipFromItem(rowStack));
                    if (panel.done != null) {
                        lines.add(panel.isDone(row)
                                ? Component.translatable("gui.bte_mobs.quest.list_done").withStyle(net.minecraft.ChatFormatting.GREEN)
                                : Component.translatable("gui.bte_mobs.quest.list_pending").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                    if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
                        lines.add(Component.translatable("gui.bte_mobs.quest.click_jei").withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                    this.renderTooltip(poseStack, lines, rowStack.getTooltipImage(), tipX, tipY, rowStack);
                    return;
                }
                if (panel.contains(mouseX, mouseY)) return;
            }
            for (ButtonReward reward : slotRewards) {
                if (reward == pinned) continue; // con el panel abierto no hace falta su tooltip
                if (reward.isMouseOver(mouseX, mouseY)) reward.drawTooltip(poseStack, mouseX, mouseY);
            }
            for (ButtonTask task : slotTask) {
                if (task == pinned) continue;
                if (task.isMouseOver(mouseX, mouseY)) task.drawTooltip(poseStack, mouseX, mouseY);
            }
        }
    }

    // ------------------------------------------------------------------ Panel de recetas de una recompensa

    /** Recompensa cuyo panel de recetas está abierto: índice dentro de slotRewards y quest a la que pertenece. */
    private int pinnedRewardIndex = -1;
    private ResourceLocation pinnedQuestId = null;
    /** true si el panel abierto es el de una tarea (lista de items válidos); false si es el de una recompensa. */
    private boolean pinnedIsTask = false;

    private static final int PANEL_ROW_HEIGHT = 18;
    private static final int PANEL_MAX_ROWS = 8;     // filas por columna antes de abrir otra columna
    private static final int PANEL_PADDING = 4;
    // Con más items que esto, el panel se muestra como cuadrícula de iconos (sin nombres; el nombre sale en el tooltip)
    private static final int PANEL_GRID_THRESHOLD = 16;
    private static final int PANEL_GRID_COLUMNS = 9;
    // Profundidad del panel. Los tooltips (vanilla y de mods) se dibujan a z >= 400,
    // así que el panel (fondo, iconos y texto) se queda por debajo de 400 para que siempre lo tapen.
    private static final int PANEL_Z = 200;

    private ButtonReward getPinnedReward() {
        if (pinnedIsTask) return null;
        if (this.currentQuest == null || pinnedQuestId == null || !pinnedQuestId.equals(this.currentQuest.id)) return null;
        if (pinnedRewardIndex < 0 || pinnedRewardIndex >= slotRewards.size()) return null;
        ButtonReward reward = slotRewards.get(pinnedRewardIndex);
        return reward.hasRecipeList() ? reward : null;
    }

    private ButtonTask getPinnedTask() {
        if (!pinnedIsTask) return null;
        if (this.currentQuest == null || pinnedQuestId == null || !pinnedQuestId.equals(this.currentQuest.id)) return null;
        if (pinnedRewardIndex < 0 || pinnedRewardIndex >= slotTask.size()) return null;
        ButtonTask task = slotTask.get(pinnedRewardIndex);
        return task.hasItemList() ? task : null;
    }

    /** Botón (recompensa o tarea) cuyo panel está abierto, o null. */
    private net.minecraft.client.gui.components.AbstractWidget getPinnedAnchor() {
        ButtonReward r = getPinnedReward();
        return r != null ? r : getPinnedTask();
    }

    private void closeRecipePanel() {
        pinnedRewardIndex = -1;
        pinnedQuestId = null;
        pinnedIsTask = false;
    }

    private static class RecipePanel {
        int x, y, colWidth, rows, cols;
        /** Modo cuadrícula: celdas de 18x18 ordenadas por filas, sin nombres. */
        boolean grid;
        java.util.List<ItemStack> items;
        /** Ids ya hechos (se marcan con ✔); null si el panel no lleva marcas (recompensas). */
        java.util.Set<String> done;
        boolean isDone(int i) {
            if (done == null) return false;
            ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(items.get(i).getItem());
            return id != null && done.contains(id.toString());
        }
        int width() { return cols * colWidth + PANEL_PADDING * 2; }
        int height() { return rows * PANEL_ROW_HEIGHT + PANEL_PADDING * 2; }
        boolean contains(double mx, double my) { return mx >= x && my >= y && mx < x + width() && my < y + height(); }
        int rowX(int i) { return x + PANEL_PADDING + (grid ? (i % cols) : (i / rows)) * colWidth; }
        int rowY(int i) { return y + PANEL_PADDING + (grid ? (i / cols) : (i % rows)) * PANEL_ROW_HEIGHT; }
        int rowAt(double mx, double my) {
            for (int i = 0; i < items.size(); i++) {
                int rx = rowX(i), ry = rowY(i);
                if (mx >= rx && my >= ry && mx < rx + colWidth && my < ry + PANEL_ROW_HEIGHT) return i;
            }
            return -1;
        }
    }

    private RecipePanel layoutRecipePanel(net.minecraft.client.gui.components.AbstractWidget anchor) {
        RecipePanel p = new RecipePanel();
        if (anchor instanceof ButtonTask task) {
            p.items = task.getListItems();
            p.done = task.getDoneIds();
        } else {
            p.items = ((ButtonReward) anchor).getRecipeItems();
        }
        if (p.items.size() > PANEL_GRID_THRESHOLD) {
            p.grid = true;
            p.cols = Math.min(PANEL_GRID_COLUMNS, p.items.size());
            p.rows = (p.items.size() + p.cols - 1) / p.cols;
            p.colWidth = PANEL_ROW_HEIGHT;
        } else {
            p.rows = Math.min(PANEL_MAX_ROWS, p.items.size());
            p.cols = (p.items.size() + PANEL_MAX_ROWS - 1) / PANEL_MAX_ROWS;
            int maxName = 0;
            for (ItemStack st : p.items) maxName = Math.max(maxName, this.font.width(st.getHoverName()));
            p.colWidth = 16 + 4 + maxName + 6 + (p.done != null ? 12 : 0);   // hueco para el ✔
        }
        // Debajo de la recompensa; si no cabe, se desplaza para quedar dentro de la pantalla
        p.x = Mth.clamp(anchor.x, 2, Math.max(2, this.width - p.width() - 2));
        p.y = anchor.y + anchor.getHeight() + 2;
        if (p.y + p.height() > this.height - 2) p.y = Math.max(2, anchor.y - p.height() - 2);
        return p;
    }

    private void renderRecipePanel(PoseStack poseStack, RecipePanel p, int mouseX, int mouseY) {
        int x0 = p.x, y0 = p.y, x1 = p.x + p.width(), y1 = p.y + p.height();
        poseStack.pushPose();
        poseStack.translate(0, 0, PANEL_Z);
        // Mismo estilo que los tooltips de Minecraft
        int bg = 0xF0100010, border1 = 0x505000FF, border2 = 0x5028007F;
        fill(poseStack, x0 + 1, y0, x1 - 1, y1, bg);
        fill(poseStack, x0, y0 + 1, x0 + 1, y1 - 1, bg);
        fill(poseStack, x1 - 1, y0 + 1, x1, y1 - 1, bg);
        fillGradient(poseStack, x0 + 1, y0 + 1, x0 + 2, y1 - 1, border1, border2);
        fillGradient(poseStack, x1 - 2, y0 + 1, x1 - 1, y1 - 1, border1, border2);
        fill(poseStack, x0 + 1, y0 + 1, x1 - 1, y0 + 2, border1);
        fill(poseStack, x0 + 1, y1 - 2, x1 - 1, y1 - 1, border2);

        int hovered = p.rowAt(mouseX, mouseY);
        for (int i = 0; i < p.items.size(); i++) {
            int rx = p.rowX(i), ry = p.rowY(i);
            if (p.grid && p.isDone(i)) fill(poseStack, rx, ry, rx + p.colWidth - 1, ry + PANEL_ROW_HEIGHT - 1, 0x6040C040);   // hecho: fondo verde
            else if (p.grid) fill(poseStack, rx, ry, rx + p.colWidth - 1, ry + PANEL_ROW_HEIGHT - 1, 0x30000000);         // hueco de la celda
            if (i == hovered) fill(poseStack, rx, ry, rx + p.colWidth - (p.grid ? 1 : 2), ry + PANEL_ROW_HEIGHT - (p.grid ? 1 : 0), 0x30FFFFFF);
        }
        poseStack.popPose();

        // Iconos: el ItemRenderer suma ~100-150 a su blitOffset, así que quedan entre el fondo y el texto (< 400)
        net.minecraft.client.renderer.entity.ItemRenderer ir = this.minecraft.getItemRenderer();
        float oldOffset = ir.blitOffset;
        ir.blitOffset = PANEL_Z;
        for (int i = 0; i < p.items.size(); i++) {
            ir.renderAndDecorateItem(p.items.get(i), p.rowX(i) + 1, p.rowY(i) + 1);
        }
        ir.blitOffset = oldOffset;

        // Nombres, por encima de los iconos pero todavía por debajo de cualquier tooltip
        poseStack.pushPose();
        poseStack.translate(0, 0, PANEL_Z + 160);
        for (int i = 0; i < p.items.size() && !p.grid; i++) {
            // En listas de tareas: hechos en verde con ✔, pendientes en gris
            int color = p.done == null ? 0xFFFFFF : (p.isDone(i) ? 0x55FF55 : 0xAAAAAA);
            this.font.drawShadow(poseStack, p.items.get(i).getHoverName(), p.rowX(i) + 20, p.rowY(i) + 5, color);
            if (p.isDone(i)) {
                this.font.drawShadow(poseStack, "\u2714", p.rowX(i) + p.colWidth - 12, p.rowY(i) + 5, 0x55FF55);
            }
        }
        poseStack.popPose();
    }
    public void renderLabel(PoseStack poseStack,int x,int y,ResourceLocation location){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0,location);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        GuiComponent.blit(poseStack, x , y , 0, 0,73, 13, 73, 13);

    }
    public void bar(PoseStack poseStack, StatTaskData statTaskData, int barX, int barY){
        poseStack.pushPose();
        poseStack.translate(barX, barY, 0.0D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_BARS_LOCATION);
        float percent = statTaskData.maxCount > 0 ? Math.min(1.0F, (float) statTaskData.count / (float) statTaskData.maxCount) : 0.0F;
        this.drawBar(poseStack, 0, 0, percent);
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
        this.stateQuest.setMessage(questStateMessage());
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
        if (p_94697_ == 0 && this.currentQuest != null) {
            net.minecraft.client.gui.components.AbstractWidget pinned = getPinnedAnchor();
            // Clic dentro del panel abierto: si es sobre un item, abre sus recetas en JEI
            if (pinned != null) {
                RecipePanel panel = layoutRecipePanel(pinned);
                if (panel.contains(p_94695_, p_94696_)) {
                    int row = panel.rowAt(p_94695_, p_94696_);
                    if (row >= 0) openInJei(panel.items.get(row));
                    return true;
                }
            }
            // Clic en una recompensa de una sola receta: abre esa receta en JEI
            for (ButtonReward reward : slotRewards) {
                if (!reward.hasRecipeList() && reward.getRecipeItems().size() == 1 && reward.isMouseOver(p_94695_, p_94696_)) {
                    openInJei(reward.getRecipeItems().get(0));
                    return true;
                }
            }
            // Clic en una recompensa de recetas: abre su panel, o lo cierra si ya estaba abierto
            for (int i = 0; i < slotRewards.size(); i++) {
                ButtonReward reward = slotRewards.get(i);
                if (reward.hasRecipeList() && reward.isMouseOver(p_94695_, p_94696_)) {
                    if (reward == pinned) {
                        closeRecipePanel();
                    } else {
                        pinnedRewardIndex = i;
                        pinnedQuestId = this.currentQuest.id;
                        pinnedIsTask = false;
                    }
                    return true;
                }
            }
            // Clic en una tarea con varios items válidos: abre la lista, o la cierra si ya estaba abierta
            for (int i = 0; i < slotTask.size(); i++) {
                ButtonTask task = slotTask.get(i);
                if (task.hasItemList() && task.isMouseOver(p_94695_, p_94696_)) {
                    if (task == pinned) {
                        closeRecipePanel();
                    } else {
                        pinnedRewardIndex = i;
                        pinnedQuestId = this.currentQuest.id;
                        pinnedIsTask = true;
                    }
                    return true;
                }
            }
            // Clic en cualquier otro sitio: cierra el panel y el clic sigue su curso normal
            if (pinned != null) closeRecipePanel();
        }
        return super.mouseClicked(p_94695_, p_94696_, p_94697_);
    }

    /** Abre las recetas del item en JEI si está instalado. */
    private void openInJei(ItemStack stack) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
            fr.shoqapik.btemobs.integration.JEIPlugin.showRecipesFor(stack);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc con el panel de recetas abierto: cierra solo el panel, no la pantalla
        if (keyCode == 256 && getPinnedAnchor() != null) {
            closeRecipePanel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
