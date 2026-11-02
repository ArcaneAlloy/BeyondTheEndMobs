package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.BlacksmithRepairMenu;
import fr.shoqapik.btemobs.packets.RenameItemPacket;
import fr.shoqapik.btemobs.api.QuestRecipeLinks;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public class BlacksmithRepairScreen extends ItemCombinerScreen<BlacksmithRepairMenu> {

    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_repair.png");
    private final Player player;
    private EditBox name;

    public BlacksmithRepairScreen(BlacksmithRepairMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, TEXTURE_LOCATION);
        this.player = pPlayerInventory.player;
        this.titleLabelX = 60;
    }


    public void containerTick() {
        super.containerTick();
        this.name.tick();
    }

    protected void subInit() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.name = new EditBox(this.font, i + 62, j + 24, 103, 12, Component.translatable("container.repair"));
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setBordered(false);
        this.name.setMaxLength(50);
        this.name.setResponder(this::onNameChanged);
        this.name.setValue("");
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(false);
    }

    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.name.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.name.setValue(s);
    }

    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.minecraft.player.closeContainer();
        }

        return !this.name.keyPressed(pKeyCode, pScanCode, pModifiers) && !this.name.canConsumeInput() ? super.keyPressed(pKeyCode, pScanCode, pModifiers) : true;
    }

    private void onNameChanged(String p_97899_) {
        if (!p_97899_.isEmpty()) {
            String s = p_97899_;
            Slot slot = this.menu.getSlot(0);
            if (slot != null && slot.hasItem() && !slot.getItem().hasCustomHoverName() && p_97899_.equals(slot.getItem().getHoverName().getString())) {
                s = "";
            }

            this.menu.setItemName(s);
            BteMobsMod.INSTANCE.sendToServer(new RenameItemPacket(s));
        }
    }

    /** Altura de la franja del aviso (mismo sitio que el coste del yunque). */
    private static final int LOCK_Y = 67;
    private static final int LOCK_H = 12;
    /** Zona clicable del nombre de la quest en el ultimo frame (coordenadas de pantalla), o null si no hay enlace. */
    private int[] questLink;
    private String questLinkId;

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        this.questLink = null;
        this.questLinkId = null;
        if (this.menu.repairLockedBy == null) {
            super.renderLabels(pPoseStack, pMouseX, pMouseY);
            return;
        }
        // Reparacion bloqueada: la franja del aviso tapa la etiqueta "Inventario", asi que solo se dibuja el titulo
        this.font.draw(pPoseStack, this.title, (float) this.titleLabelX, (float) this.titleLabelY, 4210752);

        String questId = this.menu.repairLockedBy.getId().toString();
        Component title = QuestRecipeLinks.questTitle(questId);
        if (title == null) title = Component.literal(questId);
        // Como en las recetas bloqueadas: si la quest aun no es visible para el jugador (le falta algun requisito),
        // su nombre sale censurado y no es un enlace (seria un spoiler)
        boolean openable = QuestRecipeLinks.questRequirements(questId).isEmpty() && QuestRecipeLinks.canOpenQuest(questId);

        Component prefix = Component.translatable("gui.bte_mobs.repair.locked").append(" ");
        int prefixW = this.font.width(prefix);
        int left = 8 + 3;
        int right = this.imageWidth - 8 - 3;
        int inner = right - left;

        // Posicion del nombre para saber si el raton esta encima (antes de elegir su estilo)
        Component plainName = openable ? title : censor(title.getString());
        int textW = prefixW + this.font.width(plainName);
        int offset = 0;
        int startX;
        if (textW <= inner) {
            startX = left + (inner - textW) / 2;
        } else {
            // No cabe: el texto va de un lado a otro (ida y vuelta, con pausa en cada extremo), como los botones
            int overflow = textW - inner;
            double seconds = net.minecraft.Util.getMillis() / 1000.0D;
            double period = Math.max(overflow * 0.5D, 3.0D);
            double t = Math.sin(Math.PI / 2.0D * Math.cos(Math.PI * 2.0D * seconds / period)) / 2.0D + 0.5D;
            offset = (int) Math.round(t * overflow);
            startX = left - offset;
        }
        int nameX0 = startX + prefixW;
        int nameX1 = startX + textW;
        int mx = pMouseX - this.leftPos;
        int my = pMouseY - this.topPos;
        boolean hover = openable && mx >= Math.max(nameX0, left) && mx < Math.min(nameX1, right)
                && my >= LOCK_Y && my < LOCK_Y + LOCK_H;

        Component name = openable
                ? title.copy().withStyle(hover ? ChatFormatting.YELLOW : ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                : plainName;

        fill(pPoseStack, 8, LOCK_Y, this.imageWidth - 8, LOCK_Y + LOCK_H, hover ? 0xC0330044 : 0xA0000000);

        // Recorte a la franja (coordenadas de ventana)
        com.mojang.blaze3d.platform.Window window = this.minecraft.getWindow();
        double scale = window.getGuiScale();
        int sx = (int) ((this.leftPos + left) * scale);
        int sy = (int) (window.getHeight() - (this.topPos + LOCK_Y + LOCK_H) * scale);
        int sw = (int) (inner * scale);
        int sh = (int) (LOCK_H * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
        this.font.drawShadow(pPoseStack, prefix, (float) startX, (float) (LOCK_Y + 2), 0xFF6060);
        this.font.drawShadow(pPoseStack, name, (float) nameX0, (float) (LOCK_Y + 2), 0xFF6060);
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();

        if (openable) {
            this.questLink = new int[]{this.leftPos + Math.max(nameX0, left), this.topPos + LOCK_Y,
                    this.leftPos + Math.min(nameX1, right), this.topPos + LOCK_Y + LOCK_H};
            this.questLinkId = questId;
        }
    }

    /** Fuente de la mesa de encantamientos: nombre censurado con runas que van cambiando. */
    private static final ResourceLocation ALT_FONT = new ResourceLocation("minecraft", "alt");

    private static Component censor(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(ALT_FONT)
                .applyFormat(ChatFormatting.OBFUSCATED).applyFormat(ChatFormatting.RED));
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0 && this.questLink != null && this.questLinkId != null
                && pMouseX >= this.questLink[0] && pMouseX < this.questLink[2]
                && pMouseY >= this.questLink[1] && pMouseY < this.questLink[3]) {
            // Se cierra la herrera (los objetos vuelven al inventario) y se abre la quest en el libro
            String questId = this.questLinkId;
            this.minecraft.player.closeContainer();
            if (QuestRecipeLinks.openQuest(questId, null)) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void renderFg(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.name.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual
     * contents of that slot.
     */
    public void slotChanged(AbstractContainerMenu pContainerToSend, int pSlotInd, ItemStack pStack) {
        if (pSlotInd == 0) {
            this.name.setValue(pStack.isEmpty() ? "" : pStack.getHoverName().getString());
            this.name.setEditable(!pStack.isEmpty());
            this.setFocused(this.name);
        }

    }
}
