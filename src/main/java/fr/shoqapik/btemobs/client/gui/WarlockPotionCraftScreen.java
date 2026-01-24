package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.CategoryButton;
import fr.shoqapik.btemobs.client.widget.RecipeButton;
import fr.shoqapik.btemobs.client.widget.SmithStateSwitchingButton;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.menu.WarlockPotionMenu;
import fr.shoqapik.btemobs.packets.PlaceGhostRecipePacket;
import fr.shoqapik.btemobs.packets.PlaceItemRecipePacket;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.recipe.api.IGhostRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WarlockPotionCraftScreen extends AbstractContainerScreen<WarlockPotionMenu> implements IGhostRecipe {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/cauldron_screen_1.png");
    public static final Component ALL_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
    public static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    protected static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
    private ClientRecipeBook book;

    private BteRecipeCategory currentCategory = BteRecipeCategory.ALL;
    private EditBox searchBox;
    private String previousSearchText = "";
    private List<RecipeButton> buttonList = new ArrayList<RecipeButton>();
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private List<WarlockPotionRecipe> categoryRecipes = new ArrayList<>();
    public WarlockPotionRecipe currentRecipe;
    private int page = 0;
    private RecipeButton hoveredButton;
    public final GhostRecipe ghostRecipe = new GhostRecipe();
    protected StateSwitchingButton filterButton;

    public int totalPages = 0;
    private List<CategoryButton> tabButtons = new ArrayList<>();
    public WarlockPotionCraftScreen(WarlockPotionMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.imageWidth = 339;
        this.imageHeight = 166;
        this.inventoryLabelX = 161;
        this.titleLabelX = 161;
    }

    protected void init() {
        super.init();
        this.book = minecraft.player.getRecipeBook();
        this.categoryRecipes = BteMobsMod.getListRecipe(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get());
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, i + 15, j + 14, 80, 9 + 5, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(16777215);
        this.searchBox.setValue(s);
        this.filterButton = new StateSwitchingButton(i + 100, j + 12, 26, 16, this.book.isFiltering(RecipeBookType.CRAFTING));
        this.initFilterButtonTextures();
        this.forwardButton = new SmithStateSwitchingButton(i + 93, j + 137, 12, 17, false);
        this.forwardButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        this.backButton = new SmithStateSwitchingButton(i + 38, j + 137, 12, 17, true);
        this.backButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        tabButtons.clear();
        CategoryButton button = new CategoryButton(BteRecipeCategory.ALL);
        button.active=true;
        this.tabButtons.add(button);
        this.currentRecipe=null;
        refreshButtons();
        updateTabs();
    }

    protected void initFilterButtonTextures() {
        this.filterButton.initTextureValues(152, 41, 28, 18, RECIPE_BOOK_LOCATION);
    }
    private void refreshButtons(){
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        if(page < 0) page = 0;
        if(page * 20 > categoryRecipes.size()) page -= 1;
        categoryRecipes = new ArrayList<>(BteMobsMod.getListRecipe(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get()));
        removeLockedRecipes();
        if(!searchBox.getValue().isEmpty()){
            filterRecipes();
        }
        if(this.book.isFiltering(RecipeBookType.CRAFTING)){
            updateShowRecipe();
        }else {
            reOrganize();
        }
        this.totalPages = (int)Math.ceil((double)categoryRecipes.size() / 20.0D);
        if (this.totalPages <= this.page) {
            this.page = 0;
        }
        this.buttonList.clear();
        for(int index = page * 20; index < (page+1) * 20; ++index) {
            if(index >= categoryRecipes.size()) break;
            WarlockPotionRecipe recipe = categoryRecipes.get(index);
            RecipeButton button = new RecipeButton(i+11, j+31, recipe);
            int moduloIndex = index % 20;
            button.setPosition(i + 25 * (moduloIndex % 5), j + 31 + 25 * (moduloIndex / 5));
            buttonList.add(button);
            button.setHasEnough(recipe.hasItems(this.minecraft.player,this.menu.craftSlots));
        }
        backButton.visible = page != 0 && totalPages>1;
        forwardButton.visible = page != totalPages-1 && totalPages>1;

    }

    private boolean toggleFiltering() {
        RecipeBookType recipebooktype = RecipeBookType.CRAFTING;
        boolean flag = !this.book.isFiltering(recipebooktype);
        this.book.setFiltering(recipebooktype, flag);
        return flag;
    }

    private void updateTabs() {
        int i = (this.width - 147) / 2 - 90 - 30;
        int j = (this.height - 166) / 2 + 3;
        int k = 27;
        int l = 0;

        for(CategoryButton recipebooktabbutton : this.tabButtons) {
            recipebooktabbutton.setPosition(i, j + 27 * l++);
            recipebooktabbutton.setStateTriggered(recipebooktabbutton.getCategory() == currentCategory);
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
    private void removeLockedRecipes(){
        /*List<WarlockPotionRecipe> toRemove = new ArrayList<>();
        for(WarlockPotionRecipe recipe : categoryRecipes){
            if(!ClientRecipeLocker.get().hasRecipe(recipe.getResultItem())) {
                toRemove.add(recipe);
            }
        }
        for (BlackSmithRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }*/
    }

    private void filterRecipes(){
        List<WarlockPotionRecipe> toRemove = new ArrayList<>();
        for(WarlockPotionRecipe recipe : categoryRecipes){
            if(!recipe.getResultItem().getDisplayName().getString().toLowerCase().contains(this.searchBox.getValue().toLowerCase())) {
                toRemove.add(recipe);
            }
        }
        for (WarlockPotionRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }
    }
    private void updateShowRecipe(){
        List<WarlockPotionRecipe> toRemove = new ArrayList<>();
        for(WarlockPotionRecipe recipe : categoryRecipes){
            if(!recipe.hasItems(this.minecraft.player.getInventory())) {
                toRemove.add(recipe);
            }
        }
        for (WarlockPotionRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }
    }

    private void reOrganize(){
        categoryRecipes = categoryRecipes.stream().sorted(Comparator.comparing(
                e -> e.hasItems(this.minecraft.player, this.menu.craftSlots),
                Comparator.reverseOrder())).toList();
    }

    @Override
    public void render(PoseStack p_97795_, int p_97796_, int p_97797_, float p_97798_) {
        for(CategoryButton button : tabButtons){
            button.render(p_97795_, p_97796_, p_97797_, p_97798_);
        }
        super.render(p_97795_, p_97796_, p_97797_, p_97798_);
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        if (!this.searchBox.isFocused() && this.searchBox.getValue().isEmpty()) {
            drawString(p_97795_, this.minecraft.font, SEARCH_HINT, i + 15, j + 14, -1);
        } else {
            this.searchBox.render(p_97795_, p_97796_, p_97797_, p_97798_);
        }
        hoveredButton = null;
        for(RecipeButton button : buttonList) {
            button.render(p_97795_, p_97796_ , p_97797_, p_97798_);
            if(button.isHoveredOrFocused()){
                WarlockPotionRecipe recipe = (WarlockPotionRecipe) button.getRecipe();
                button.requieredStacks.put(recipe.getIngredientPrimary().getItem().toString(),recipe.getIngredientPrimary().getCount());
                hoveredButton = button;
            }
        }

        this.renderGhostRecipe(p_97795_, this.leftPos, this.topPos, false, p_97798_);

        this.backButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        this.forwardButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        if(previousSearchText != searchBox.getValue()){
            previousSearchText = searchBox.getValue();
            page = 0;
        }
        this.filterButton.render(p_97795_, p_97796_, p_97797_, p_97798_);

        refreshButtons();
        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.renderGhostRecipeTooltip(p_97795_, this.leftPos, this.topPos, p_97796_,p_97797_);
    }

    public void renderTooltip(PoseStack p_100418_, int p_100419_, int p_100420_) {
        if (this.minecraft.screen != null && this.hoveredButton != null) {
            this.minecraft.screen.renderComponentTooltip(p_100418_, this.hoveredButton.getTooltipText(this.minecraft.screen), p_100419_, p_100420_, this.hoveredButton.getRecipeStack());
        }
        if (this.filterButton.isHoveredOrFocused()) {
            Component component = this.getFilterButtonTooltip();
            if (this.minecraft.screen != null) {
                this.minecraft.screen.renderTooltip(p_100418_, component, p_100419_, p_100420_);
            }
        }
        super.renderTooltip(p_100418_,p_100419_,p_100420_);
    }

    private Component getFilterButtonTooltip() {
        return this.filterButton.isStateTriggered() ? this.getRecipeFilterName() : ALL_RECIPES_TOOLTIP;
    }

    protected Component getRecipeFilterName() {
        return ONLY_CRAFTABLES_TOOLTIP;
    }
    @Override
    protected void renderBg(PoseStack p_97787_, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_LOCATION);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;
        blit(p_97787_, i, j, 0, 0, this.imageWidth, this.imageHeight, 512, 512);
    }

    protected void renderLabels(PoseStack p_97808_, int p_97809_, int p_97810_) {
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean flag=false;
        for (WarlockPotionRecipe recipe : this.categoryRecipes){
            if(this.menu.recipeMatches(recipe)){
                flag=true;
                this.currentRecipe= recipe;
                break;
            }
        }
        if(!flag){
            this.currentRecipe=null;
        }
        this.searchBox.tick();
    }

    public void renderGhostRecipe(PoseStack pPoseStack, int pLeftPos, int pTopPos, boolean p_100326_, float pPartialTick) {
        this.ghostRecipe.render(pPoseStack, this.minecraft, pLeftPos, pTopPos, p_100326_, pPartialTick);
    }
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
        if (pSlot != null && pSlot.index < this.menu.craftSlots.getContainerSize()) {
            this.ghostRecipe.clear();
        }
    }
    @Override
    public boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_) {
        if(this.searchBox.mouseClicked(p_97748_, p_97749_, p_97750_)){
            return true;
        }else if(forwardButton.mouseClicked(p_97748_, p_97749_, p_97750_)){
            page+=1;
            refreshButtons();
            return true;
        }else if(backButton.mouseClicked(p_97748_, p_97749_, p_97750_)){
            page-=1;
            refreshButtons();
            return true;
        } else if (this.filterButton.mouseClicked(p_97748_, p_97749_, p_97750_)) {
            boolean flag = this.toggleFiltering();
            this.filterButton.setStateTriggered(flag);
            this.sendUpdateSettings();
            this.page=0;
            refreshButtons();
            return true;
        }
        for(CategoryButton button : tabButtons){
            if(button.mouseClicked(p_97748_, p_97749_, p_97750_)){
                if(button.getCategory() != currentCategory) {
                    this.searchBox.setValue("");
                    this.page = 0;
                    this.currentCategory = button.getCategory();
                    refreshButtons();
                    updateTabs();
                }
            }
        }
        for(RecipeButton button : buttonList) {
            if (button.mouseClicked(p_97748_, p_97749_, p_97750_)) {
                if(button.hasEnough){
                    BteMobsMod.sendToServer(new PlaceItemRecipePacket(((WarlockPotionRecipe)button.getRecipe()).getIngredientPrimary()));
                    this.currentRecipe= (WarlockPotionRecipe) button.getRecipe();
                }else {
                    Recipe<?> recipe = button.getRecipe();
                    if (recipe != null ) {
                        this.ghostRecipe.clear();
                        //BteMobsMod.sendToServer(new PlaceGhostRecipePacket(this.menu.containerId,recipe));
                    }
                }
            }
        }
        refreshButtons();
        return super.mouseClicked(p_97748_, p_97749_, p_97750_);
    }
    protected void sendUpdateSettings() {
        if (this.minecraft.getConnection() != null) {
            RecipeBookType recipebooktype = RecipeBookType.CRAFTING;
            boolean flag1 = this.book.getBookSettings().isFiltering(recipebooktype);
            this.minecraft.getConnection().send(new ServerboundRecipeBookChangeSettingsPacket(recipebooktype, true, flag1));
        }

    }
    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
        refreshButtons();
        if(this.searchBox.keyPressed(p_97765_, p_97766_, p_97767_)){
            return true;
        }
        return searchBox.isFocused() || super.keyPressed(p_97765_, p_97766_, p_97767_);
    }

    @Override
    public boolean charTyped(char p_94683_, int p_94684_) {
       if(this.searchBox.charTyped(p_94683_, p_94684_)){
           page = 0;
           refreshButtons();
           return true;
       }
       return super.charTyped(p_94683_, p_94684_);
    }

    @Override
    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);
        this.ghostRecipe.addIngredient(Ingredient.of(Items.GLASS_BOTTLE),pSlots.get(2).x,pSlots.get(2).y);
        this.ghostRecipe.addIngredient(Ingredient.of(((WarlockPotionRecipe)pRecipe).getIngredientPrimary()),pSlots.get(3).x,pSlots.get(3).y);
    }
    @Override
    public void removed() {
        super.removed();
        this.book.setBookSetting(RecipeBookType.CRAFTING,false,false);
    }
}
