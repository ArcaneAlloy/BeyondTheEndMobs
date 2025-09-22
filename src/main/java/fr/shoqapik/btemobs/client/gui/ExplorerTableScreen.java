package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.CategoryButton;
import fr.shoqapik.btemobs.client.widget.RecipeButton;
import fr.shoqapik.btemobs.client.widget.SmithStateSwitchingButton;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.menu.TableExplorerMenu;
import fr.shoqapik.btemobs.packets.PlaceItemRecipePacket;
import fr.shoqapik.btemobs.packets.StartCraftingItemPacket;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.recipe.api.IGhostRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static fr.shoqapik.btemobs.client.gui.WarlockPotionCraftScreen.*;

public class ExplorerTableScreen extends AbstractContainerScreen<TableExplorerMenu> implements IGhostRecipe {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    private BteRecipeCategory currentCategory = BteRecipeCategory.ALL;
    private EditBox searchBox;
    private String previousSearchText = "";
    private List<RecipeButton> buttonList = new ArrayList<RecipeButton>();
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private List<ExplorerRecipe> categoryRecipes = new ArrayList<>();
    public ExplorerRecipe currentRecipe;
    private int page = 0;
    private RecipeButton hoveredButton;
    private List<CategoryButton> tabButtons = new ArrayList<>();
    private Button craftButton;
    public final GhostRecipe ghostRecipe = new GhostRecipe();
    public int totalPages = 0;
    private ClientRecipeBook book;
    protected StateSwitchingButton filterButton;

    public ExplorerTableScreen(TableExplorerMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.imageWidth = 329;
        this.imageHeight = 166;
        this.inventoryLabelX = 161;
        this.titleLabelX = 161;
    }

    protected void init() {
        super.init();
        this.book = minecraft.player.getRecipeBook();
        this.categoryRecipes = BteMobsMod.getListRecipe(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get());
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, i + 20, j + 14, 80, 9 + 5, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(16777215);
        this.searchBox.setValue(s);
        this.filterButton = new StateSwitchingButton(i + 105, j + 12, 26, 16, this.book.isFiltering(RecipeBookType.CRAFTING));
        this.initFilterButtonTextures();
        this.forwardButton = new SmithStateSwitchingButton(i + 93, j + 137, 12, 17, false);
        this.forwardButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        this.backButton = new SmithStateSwitchingButton(i + 38, j + 137, 12, 17, true);
        this.backButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        tabButtons.clear();
        this.tabButtons.add(new CategoryButton(BteRecipeCategory.ALL));
        this.currentRecipe=null;
        refreshButtons();
        updateTabs();
        this.craftButton = new Button(this.leftPos + 290, (this.height / 2 - this.imageHeight / 2) + 64, 35, 14, Component.literal("Craft"), p_93751_ -> {
            if(this.currentRecipe!=null){
                Entity entity=Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
                if(entity instanceof ExplorerEntity e){
                    ItemStack stack=this.menu.assemble(this.currentRecipe);
                    e.startCrafting(stack);
                    BteMobsMod.sendToServer(new StartCraftingItemPacket(stack,e.getId()));
                }

                Minecraft.getInstance().setScreen(null);
            }
        });
        this.craftButton.active = false;
    }
    protected void initFilterButtonTextures() {
        this.filterButton.initTextureValues(152, 41, 28, 18, RECIPE_BOOK_LOCATION);
    }
    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);
        for (int i = 0 ; i < pRecipe.getIngredients().size() ; i++){
            if(this.menu.craftSlots.getItem(i).isEmpty()){
                this.ghostRecipe.addIngredient(pRecipe.getIngredients().get(i),pSlots.get(i).x,pSlots.get(i).y);
            }
        }
    }

    private void refreshButtons(){
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        if(page < 0) page = 0;
        if(page * 20 > categoryRecipes.size()) page -= 1;
        categoryRecipes =new ArrayList<>( BteMobsMod.getListRecipe(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get()));
        removeLockedRecipes();
        if(!searchBox.getValue().isEmpty()){
            filterRecipes();
        }
        if(this.book.isFiltering(RecipeBookType.CRAFTING)){
            updateShowRecipe();
        }else {
            reOrganize();
        }
        this.buttonList.clear();
        for(int index = page * 20; index < (page+1) * 20; ++index) {
            if(index >= categoryRecipes.size()) break;
            ExplorerRecipe recipe = categoryRecipes.get(index);
            RecipeButton button = new RecipeButton(i+11, j+31, recipe);
            int moduloIndex = index % 20;
            button.setPosition(i + 6 + 25 * (moduloIndex % 5), j + 31 + 25 * (moduloIndex / 5));
            buttonList.add(button);
            button.setHasEnough(recipe.hasItems(this.minecraft.player));
        }
        this.totalPages = (int)Math.ceil((double)categoryRecipes.size() / 20.0D);
        if (this.totalPages <= this.page) {
            this.page = 0;
        }
        backButton.visible = page != 0 && totalPages>1;
        forwardButton.visible = page != totalPages-1 && totalPages>1;

    }
    private void updateShowRecipe(){
        List<ExplorerRecipe> toRemove = new ArrayList<>();
        for(ExplorerRecipe recipe : categoryRecipes){
            if(!recipe.hasItems(this.minecraft.player)) {
                toRemove.add(recipe);
            }
        }
        for (ExplorerRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }
    }

    private void reOrganize(){
        categoryRecipes = categoryRecipes.stream().sorted(Comparator.comparing(
                e -> e.hasItems(this.minecraft.player),
                Comparator.reverseOrder())).toList();
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

    private void removeLockedRecipes(){
        /*List<ExplorerRecipe> toRemove = new ArrayList<>();
        for(ExplorerRecipe recipe : categoryRecipes){
            if(!ClientRecipeLocker.get().hasRecipe(recipe.getResultItem())) {
                toRemove.add(recipe);
            }
        }
        for (BlackSmithRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }*/
    }
    protected void sendUpdateSettings() {
        if (this.minecraft.getConnection() != null) {
            RecipeBookType recipebooktype = RecipeBookType.CRAFTING;
            boolean flag1 = this.book.getBookSettings().isFiltering(recipebooktype);
            this.minecraft.getConnection().send(new ServerboundRecipeBookChangeSettingsPacket(recipebooktype, true, flag1));
        }

    }
    private void filterRecipes(){
        List<ExplorerRecipe> toRemove = new ArrayList<>();
        for(ExplorerRecipe recipe : categoryRecipes){
            if(!recipe.getResultItem().getDisplayName().getString().toLowerCase().contains(this.searchBox.getValue().toLowerCase())) {
                toRemove.add(recipe);
            }
        }
        for (ExplorerRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }
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
            drawString(p_97795_, this.minecraft.font, SEARCH_HINT, i + 20, j + 14, -1);
        } else {
            this.searchBox.render(p_97795_, p_97796_, p_97797_, p_97798_);
        }
        if (this.totalPages > 1) {
            String s = this.page + 1 + "/" + this.totalPages;
            int i1 = this.minecraft.font.width(s);

            this.minecraft.font.draw(p_97795_, s, (float)(i-i1 / 2 + 73), (float)(j + 141), -1);
        }
        hoveredButton = null;
        for(RecipeButton button : buttonList) {
            button.render(p_97795_, p_97796_, p_97797_, p_97798_);
            if(button.isHoveredOrFocused()){
                ExplorerRecipe recipe = (ExplorerRecipe) button.getRecipe();
                for (ItemStack stack : recipe.getRequiredItems().getItems()){
                    button.requieredStacks.put(stack.getItem().toString(),stack.getCount());
                }
                hoveredButton = button;
            }
        }
        this.renderGhostRecipe(p_97795_, this.leftPos, this.topPos, false, p_97798_);

        this.backButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        this.forwardButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        if(previousSearchText != searchBox.getValue()){
            previousSearchText = searchBox.getValue();
            page = 0;
            refreshButtons();
        }
        this.filterButton.render(p_97795_, p_97796_, p_97797_, p_97798_);

        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.craftButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
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
        for (ExplorerRecipe recipe : this.categoryRecipes){
            if(this.menu.recipeMatches(recipe) && this.hasExplorerFree()){
                flag=true;
                this.currentRecipe= recipe;
                this.craftButton.active=true;
                break;
            }
        }
        if(!flag){
            this.currentRecipe=null;
            this.craftButton.active=false;
        }
        this.searchBox.tick();
    }

    public boolean hasExplorerFree(){
        Entity entity =Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
        return entity instanceof ExplorerEntity explorer && !explorer.isCrafting() && entity.isAlive();
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
        } else if (this.craftButton.mouseClicked(p_97748_, p_97749_, p_97750_)) {
            return true;
        }else if (this.filterButton.mouseClicked(p_97748_, p_97749_, p_97750_)) {
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
            if (button.mouseClicked(p_97748_, p_97749_, p_97750_) && this.hasExplorerFree()) {
                if(button.hasEnough){
                    BteMobsMod.sendToServer(new PlaceItemRecipePacket(button.getRecipe().getResultItem()));
                    this.currentRecipe= (ExplorerRecipe) button.getRecipe();
                    this.craftButton.active = true;
                }else {
                    Recipe<?> recipe = button.getRecipe();
                    if (recipe != null ) {
                        this.ghostRecipe.clear();
                        BteMobsMod.sendToServer(new PlaceItemRecipePacket(button.getRecipe().getResultItem()));
                    }
                }
            }
        }
        return super.mouseClicked(p_97748_, p_97749_, p_97750_);
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
}
