package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.WarlockUpgradeMenu;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.IGhostRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.List;

public class WarlockUpgradeScreen extends AbstractContainerScreen<WarlockUpgradeMenu> implements IGhostRecipe {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_upgrade_screen.png");

    private Button modeButton;

    public WarlockRecipe currentRecipe = null;
    private int page = 0;
    private fr.shoqapik.btemobs.client.widget.RecipeButton hoveredButton;
    private List<Enchantment> enchantments = new ArrayList<>();
    public final GhostRecipe ghostRecipe = new GhostRecipe();

    protected StateSwitchingButton filterButton;
    private final Player player;
    protected int scrolledY=0;
    protected Enchantment currentEnchant=null;
    public int lastMode = 0;
    public WarlockUpgradeScreen(WarlockUpgradeMenu p_97741_, Inventory p_97742_, Component p_97743_) {
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

        this.layoutButtons();

        this.modeButton = this.addRenderableWidget(new Button(this.leftPos+142,this.topPos + 29,65,16,Component.literal("Upgrade"),(p)->{
            p.active = false;
            boolean isUpgradeMode = menu.mode.get() == 0;
            menu.mode.set(isUpgradeMode ? 1 : 0);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, isUpgradeMode? 1 : 0 );
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
        }));
        if (this.menu.getSlot(0).hasItem()){
            createButtonEnchants(this.menu.getSlot(0).getItem());
        }
        lastMode = -1;
        refreshButtons();
        layoutButtons();
    }

    protected void createButtonEnchants(ItemStack stack){
        enchantments.clear();
        enchantments.addAll(EnchantmentHelper.getEnchantments(stack).keySet());
        if (menu.mode.get() == 1)return;
    }


    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);
//        for (int i = 0 ; i < pRecipe.getIngredients().size() ; i++){
//            if(this.menu.craftSlots.getItem(i).isEmpty()){
//                this.ghostRecipe.addIngredient(pRecipe.getIngredients().get(i),pSlots.get(i).x,pSlots.get(i).y);
//            }
//        }
    }

    private void refreshButtons(){
        if(page < 0) page = 0;

        modeButton.active = menu.canUpgrade(player.getInventory(),currentRecipe) && currentEnchant != null;
    }




    @Override
    public void render(PoseStack p_97795_, int p_97796_, int p_97797_, float p_97798_) {
        super.render(p_97795_, p_97796_, p_97797_, p_97798_);

        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;

        hoveredButton = null;
        this.renderGhostRecipe(p_97795_, this.leftPos, this.topPos, false, p_97798_);

//        this.backButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
//        this.forwardButton.render(p_97795_, p_97796_, p_97797_, p_97798_);


        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.renderGhostRecipeTooltip(p_97795_, this.leftPos, this.topPos, p_97796_,p_97797_);
        modeButton.active = menu.getSlot(0).hasItem();


        if (this.menu.mode.get()!=this.lastMode){
            this.lastMode = menu.mode.get();
            boolean isUpgradeMode = menu.mode.get() == 0;
            modeButton.setMessage(Component.literal(isUpgradeMode ? "Upgrade" : "Downgrade"));
            if (isUpgradeMode){
                ListTag listTag = EnchantedBookItem.getEnchantments(menu.inputSlots.getItem(0));

                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(EnchantmentHelper.getEnchantmentId(listTag.getCompound(0)));
                Optional<WarlockRecipe> optional = minecraft.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get())

                        .stream().filter(e -> {
                            if (enchantment == e.getEnchantment()){
                                if(EnchantmentHelper.getEnchantmentLevel(listTag.getCompound(0))+1==e.getLevel()){
                                    return menu.canUpgrade(player.getInventory(),e);
                                }
                            }
                            return false;
                        }).findFirst();
                optional.ifPresent((r)->currentRecipe = r);
            }else {
                currentRecipe = null;
            }
        }
        Component component = Component.literal("Need Skeleton Skull : ").append(String.valueOf(this.currentRecipe==null ? 5 :this.currentRecipe.needEyes));
        Component component1 = Component.literal(this.currentRecipe==null ? "Up  +" :"Need XP : ").append(String.valueOf(this.currentRecipe==null ? 1 +" level": this.currentRecipe.getExperience()));

        int color = 8453920;
        if (menu.mode.get() == 0){
            if (currentRecipe == null){
                color = 16736352;
                component = Component.literal("Can't found next enchant level");
                component1 = null;
            }
        }else {
            if (menu.getEnchantLevel(menu.inputSlots.getItem(0),0)==1){
                component = Component.literal("Minimum Enchant Level");
                component1 = null;
            }
        }
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
        if (this.minecraft.screen != null && this.hoveredButton != null) {
            this.minecraft.screen.renderComponentTooltip(p_100418_, this.hoveredButton.getTooltipText(this.minecraft.screen), p_100419_, p_100420_, this.hoveredButton.getRecipeStack());
        }


        super.renderTooltip(p_100418_,p_100419_,p_100420_);
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

    }

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
        this.currentRecipe = null;
        this.lastMode = -1;
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

    private void layoutButtons() {


    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (this.enchantments.size()>7) {
            int j = this.enchantments.size() - 7;
            this.scrolledY = Mth.clamp((int)((double)this.scrolledY - pDelta), 0, j);
            this.layoutButtons();
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

}
