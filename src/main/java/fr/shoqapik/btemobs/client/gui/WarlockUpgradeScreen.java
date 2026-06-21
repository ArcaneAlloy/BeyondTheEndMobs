package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.EnchantType;
import fr.shoqapik.btemobs.button.CustomButton;
import fr.shoqapik.btemobs.client.widget.CategoryButton;
import fr.shoqapik.btemobs.client.widget.EnchantTypeButton;
import fr.shoqapik.btemobs.client.widget.RecipeButton;
import fr.shoqapik.btemobs.client.widget.SmithStateSwitchingButton;
import fr.shoqapik.btemobs.menu.WarlockCraftMenu;
import fr.shoqapik.btemobs.menu.WarlockUpgradeMenu;
import fr.shoqapik.btemobs.packets.PlaceItemRecipePacket;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.recipe.api.IGhostRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;

import java.util.*;

import static fr.shoqapik.btemobs.client.gui.WarlockPotionCraftScreen.*;

public class WarlockUpgradeScreen extends AbstractContainerScreen<WarlockUpgradeMenu> implements IGhostRecipe {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_upgrade_screen.png");

    private Button upgradeButton;
    private Button downgradeButton;
    public WarlockRecipe currentRecipe;
    private int page = 0;
    private fr.shoqapik.btemobs.client.widget.RecipeButton hoveredButton;
    private List<Button> buttons = new ArrayList<>();
    private List<Enchantment> enchantments = new ArrayList<>();
    public final GhostRecipe ghostRecipe = new GhostRecipe();

    protected StateSwitchingButton filterButton;
    private final Player player;
    protected Button up;
    protected Button down;
    protected int scrolledY=0;
    protected Enchantment currentEnchant=null;
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

        this.up = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height -80)-150,14,16,0,0,0,
                new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/up.png"),14,16,(p)->{
            scrolledY = Math.max(0,scrolledY-1);
            this.layoutButtons();
        });

        this.down = new ImageButton((this.leftPos - (this.width / 8))+20,(this.height - 80)+45,14,16,0,0,0,
                new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/explorer/down.png"),14,16,(p)->{
            scrolledY=Math.min(this.enchantments.size(),this.scrolledY+1);
            this.layoutButtons();
        });

        this.addRenderableWidget(this.up);
        this.addRenderableWidget(this.down);
        this.layoutButtons();
        this.currentRecipe=null;
        this.upgradeButton = this.addRenderableWidget(new Button(this.leftPos+140,50,75,16,Component.literal("Upgrade X"),(p)->{
            p.active = false;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);

        }));
        this.downgradeButton = this.addRenderableWidget(new Button(this.leftPos+140,90,75,16,Component.literal("Downgrade"),(p)->{
            p.active = false;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);

        }));
        if (this.menu.getSlot(0).hasItem()){
            createButtonEnchants(this.menu.getSlot(0).getItem());
        }
        refreshButtons();
        layoutButtons();
    }

    protected void createButtonEnchants(ItemStack stack){
        enchantments.clear();
        buttons.clear();
        enchantments.addAll(EnchantmentHelper.getEnchantments(menu.inputSlots.getItem(0)).keySet());

        int i = 0;
        for (Enchantment enchantment : this.enchantments) {
            ResourceLocation backgroundTexture = new ResourceLocation(BteMobsMod.MODID, String.format("textures/gui/buttons/warlock/background.png"));
            String rawTitle = EnchantmentHelper.getEnchantmentId(enchantment).toString().split(":")[1];
            String translatedTitle = rawTitle.contains(".") ? I18n.get(rawTitle) : rawTitle;

            int finalI = i;
            CustomButton button = new CustomButton(backgroundTexture, null , 0, 0, 100, 20, Component.literal(translatedTitle),
                    (p_95981_) -> {
                        this.currentEnchant = enchantment;
                        buttons.forEach((b)->{
                            if (b instanceof CustomButton){
                                ((CustomButton) b).isSelect = false;
                            }
                        });
                        if (p_95981_ instanceof CustomButton customButton){
                            customButton.isSelect = true;
                        }
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3+finalI);
                        if(menu.mode.get() == 0){
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                        }else if(menu.mode.get() == 1){
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                        }

                        Optional<WarlockRecipe> optional =
                                BteMobsMod.getWarlockRecipe(player)
                                        .stream()
                                        .filter(e -> {
                                            BteMobsMod.LOGGER.info("Enchantment {} Level {}",e.getEnchantment(),e.getLevel());

                                            if (enchantment == e.getEnchantment()) {
                                                return EnchantmentHelper.getTagEnchantmentLevel(e.getEnchantment(), menu.inputSlots.getItem(0)) + 1 == e.getLevel() && menu.canUpgrade(player.getInventory(), e);
                                            }
                                            return false;
                                        })
                                        .findFirst();

                        currentRecipe = optional.orElse(null);
            }
            );
            button.isSelect = i==0;
            currentEnchant = enchantment;
            i++;
            buttons.add(button);
        }


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

        upgradeButton.active = menu.canUpgrade(player.getInventory(),currentRecipe) && currentEnchant != null;
        downgradeButton.active = menu.canDowngrade(player.getInventory()) && currentEnchant != null;
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

        this.up.render(p_97795_, p_97796_, p_97797_, p_97798_);
        this.down.render(p_97795_, p_97796_, p_97797_, p_97798_);


        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.renderGhostRecipeTooltip(p_97795_, this.leftPos, this.topPos, p_97796_,p_97797_);
        upgradeButton.active = menu.canUpgrade(player.getInventory());
        downgradeButton.active = menu.canDowngrade(player.getInventory());
        for (Button b : buttons){
            b.render(p_97795_, p_97796_, p_97797_, p_97798_);
        }
        if(this.currentRecipe != null){
            ItemStack item = Items.SKELETON_SKULL.getDefaultInstance();
            item.setCount(this.currentRecipe.needEyes);
            Minecraft.getInstance().getItemRenderer().renderGuiItem(item,this.upgradeButton.x+50,this.upgradeButton.y);
            Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font,item,this.upgradeButton.x+50,this.upgradeButton.y);
        }

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


    }

//    public boolean hasExplorerFree(){
//        Entity entity = Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
//        return entity !=null && entity.isAlive();
//    }

    @Override
    public boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_) {

        for (Button b : buttons){
            if (!b.mouseClicked(p_97748_, p_97749_, p_97750_)){
                continue;
            }
            return true;
        }

        if(this.down.mouseClicked(p_97748_, p_97749_, p_97750_)){
            return true;
        }else if(this.up.mouseClicked(p_97748_, p_97749_, p_97750_)){
            return true;
        }

        return super.mouseClicked(p_97748_, p_97749_, p_97750_);
    }

    public void renderGhostRecipe(PoseStack pPoseStack, int pLeftPos, int pTopPos, boolean p_100326_, float pPartialTick) {
        this.ghostRecipe.render(pPoseStack, this.minecraft, pLeftPos, pTopPos, p_100326_, pPartialTick);
    }
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        if (menu.getSlot(0).hasItem()){
            createButtonEnchants(menu.getSlot(0).getItem());
            layoutButtons();
        }else{
            enchantments.clear();
            buttons.clear();
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

    private void layoutButtons() {
        int x = (int) (this.leftPos - (this.width / 8) +10);
        int yStart = (int) (this.height - 80 - 130);

        int visibleStartIndex = scrolledY;
        int visibleEndIndex = Math.min(scrolledY + 7, enchantments.size());

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

        this.down.visible = visibleEndIndex < enchantments.size();
        this.down.active = visibleEndIndex < enchantments.size();
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
