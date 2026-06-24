package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.client.widget.RecipeButton;
import fr.shoqapik.btemobs.menu.CurseRemovalMenu;

import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.IGhostRecipe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

import static fr.shoqapik.btemobs.client.gui.WarlockPotionCraftScreen.*;

public class WarlockCursesRemoveScreen extends AbstractContainerScreen<CurseRemovalMenu> implements IGhostRecipe {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_upgrade_screen.png");
    public WarlockRecipe currentRecipe;
    private int page = 0;
    private RecipeButton hoveredButton;
    private List<Button> buttons = new ArrayList<>();
    private List<Enchantment> curses = new ArrayList<>();
    public final GhostRecipe ghostRecipe = new GhostRecipe();

    protected StateSwitchingButton filterButton;
    private final Player player;
    protected int scrolledY=0;
    protected Enchantment currentCurse=null;
    public WarlockCursesRemoveScreen(CurseRemovalMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.imageWidth = 306;
        this.imageHeight = 166;
        this.inventoryLabelX = 161;
        this.titleLabelX = 161;
        this.player = p_97742_.player;
    }

    protected void init() {
        super.init();
        this.leftPos = (this.width - 147) / 2 - 86;
        this.topPos = (this.height - 166) / 2;

        this.currentRecipe=null;
        if (this.menu.getSlot(0).hasItem()){
            createButtonCurses(this.menu.getSlot(0).getItem());
        }
        refreshButtons();
    }

    protected void createButtonCurses(ItemStack stack){

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
    }

    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);

    }

    private void refreshButtons(){

        if(page < 0) page = 0;

//        upgradeButton.active = menu.canUpgrade(player.getInventory());
//        downgradeButton.active = menu.canDowngrade(player.getInventory());
    }

    @Override
    public void render(PoseStack p_97795_, int p_97796_, int p_97797_, float p_97798_) {
        super.render(p_97795_, p_97796_, p_97797_, p_97798_);

        hoveredButton = null;
        this.renderGhostRecipe(p_97795_, this.leftPos, this.topPos, false, p_97798_);



        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.renderGhostRecipeTooltip(p_97795_, this.leftPos, this.topPos, p_97796_,p_97797_);

        Component component = Component.literal("Need Skeleton Skull : ").append(String.valueOf(this.currentRecipe==null ? 5 :this.currentRecipe.needEyes));
        Component component1 = Component.literal("Need XP : ").append(20 + " level");

        int color = 8453920;
        if (!this.menu.getSlot(0).hasItem()) {
            component = null;
            component1 = null;
        }
        p_97795_.pushPose();
        p_97795_.scale(0.8F, 0.8F, 0.8F);
        if (component != null) {
            int k = this.leftPos - 8 - this.font.width(component) - 2;
            this.font.drawShadow(p_97795_, component, (float)k + 305, topPos+85, color);
        }
        if (component1!=null){
            int k = this.leftPos - 8 - this.font.width(component1) - 2;
            this.font.drawShadow(p_97795_, component1, (float)k+275, topPos+75, color);
        }
        p_97795_.popPose();
    }

    public void renderTooltip(PoseStack p_100418_, int p_100419_, int p_100420_) {
        super.renderTooltip(p_100418_,p_100419_,p_100420_);
        for (Button b : buttons){
            if (!b.isHoveredOrFocused())continue;
            b.renderToolTip(p_100418_,p_100419_,p_100420_);
        }
    }

    private void renderGhostRecipeTooltip(PoseStack pPoseStack, int p_100376_, int p_100377_, int pMouseX, int pMouseY) {
        ItemStack itemstack = null;

        for(int i = 0; i < this.ghostRecipe.size(); ++i) {
            GhostRecipe.GhostIngredient ghostrecipe$ghostingredient = this.ghostRecipe.get(i);
            int j = ghostrecipe$ghostingredient.getX() + p_100376_;
            int k = ghostrecipe$ghostingredient.getY() + p_100377_;
            if (pMouseX >= j && pMouseY >= k && pMouseX < j + 16 && pMouseY < k + 16) {
                itemstack = ghostrecipe$ghostingredient.getItem();
            }
        }

        if (itemstack != null && this.minecraft.screen != null) {
            this.minecraft.screen.renderComponentTooltip(pPoseStack, this.minecraft.screen.getTooltipFromItem(itemstack), pMouseX, pMouseY, itemstack);
        }

    }

    @Override
    protected void renderBg(PoseStack p_97787_, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION);
        int i = this.leftPos + 176 -90;
        int j = (this.height - this.imageHeight) / 2;
        blit(p_97787_, i, j, 0, 0, this.imageWidth +176 , this.imageHeight, 512, 512);
    }

    protected void renderLabels(PoseStack pPoseStack, int pX, int pY) {
        RenderSystem.disableBlend();
//        int i = this.menu.experience.get();
//        if (i > 0) {
//            int j = 8453920;
//            Component component;
//            if (i >= 40 && !this.minecraft.player.getAbilities().instabuild) {
//                component = TOO_EXPENSIVE_TEXT;
//                j = 16736352;
//            } else if (!this.menu.getSlot(4).hasItem()) {
//                component = null;
//            } else {
//                component = Component.translatable("container.repair.cost", i);
//                if (!this.menu.getSlot(4).mayPickup(this.player)) {
//                    j = 16736352;
//                }
//            }
//
//            if (component != null) {
//                int k = this.imageWidth - 8 - this.font.width(component) - 2;
//                int l = 69;
//                fill(pPoseStack, k - 2, 67, this.imageWidth - 8, 79, 1325400064);
//                this.font.drawShadow(pPoseStack, component, (float)k, 69.0F, j);
//            }
//        }
        RenderSystem.enableBlend();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean flag=false;
//        for (WarlockRecipe recipe : this.categoryRecipes){
//            if(this.menu.recipeMatches(recipe) && this.hasExplorerFree()){
//                flag=true;
//                this.currentRecipe= recipe;
//                break;
//            }
//        }
        if(!flag){
            this.currentRecipe=null;
        }

    }

//    public boolean hasExplorerFree(){
//        Entity entity = Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
//        return entity !=null && entity.isAlive();
//    }

    @Override
    public boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_) {

        return super.mouseClicked(p_97748_, p_97749_, p_97750_);
    }

    public void renderGhostRecipe(PoseStack pPoseStack, int pLeftPos, int pTopPos, boolean p_100326_, float pPartialTick) {
        this.ghostRecipe.render(pPoseStack, this.minecraft, pLeftPos, pTopPos, p_100326_, pPartialTick);
    }

    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        if (menu.getSlot(0).hasItem()){
            createButtonCurses(menu.getSlot(0).getItem());
        }

//        if (pSlot != null && pSlot.index < this.menu.craftSlots.getContainerSize()) {
//            this.ghostRecipe.clear();
//        }
    }
    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
        refreshButtons();

        return super.keyPressed(p_97765_, p_97766_, p_97767_);
    }

    @Override
    public boolean charTyped(char p_94683_, int p_94684_) {
//        if(this.searchBox.charTyped(p_94683_, p_94684_)){
//            page = 0;
//            refreshButtons();
//            return true;
//        }
        return super.charTyped(p_94683_, p_94684_);
    }

    @Override
    public void removed() {
        super.removed();
    }


    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (this.curses.size()>7) {
            int j = this.curses.size() - 7;
            this.scrolledY = Mth.clamp((int)((double)this.scrolledY - pDelta), 0, j);

        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

}
