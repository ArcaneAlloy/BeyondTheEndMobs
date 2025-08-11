package fr.shoqapik.btemobs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.widget.CategoryButton;
import fr.shoqapik.btemobs.client.widget.ExplorerTableRecipeButton;
import fr.shoqapik.btemobs.client.widget.SmithStateSwitchingButton;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.menu.DruidMenu;
import fr.shoqapik.btemobs.menu.TableExplorerMenu;
import fr.shoqapik.btemobs.packets.PlaceItemRecipePacket;
import fr.shoqapik.btemobs.packets.StartCraftingItemPacket;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DruidScreen extends AbstractContainerScreen<DruidMenu> {
    public static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");
    private BteRecipeCategory currentCategory = BteRecipeCategory.ALL;
    private EditBox searchBox;
    private String previousSearchText = "";
    private List<ExplorerTableRecipeButton> buttonList = new ArrayList<ExplorerTableRecipeButton>();
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private List<DruidRecipe> categoryRecipes = new ArrayList<>();
    public DruidRecipe currentRecipe;
    private int page = 0;
    private ExplorerTableRecipeButton hoveredButton;
    private List<CategoryButton> tabButtons = new ArrayList<>();
    private Button craftButton;
    public DruidScreen(DruidMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        this.imageWidth = 329;
        this.imageHeight = 166;
        this.inventoryLabelX = 161;
        this.titleLabelX = 161;
    }

    protected void init() {
        super.init();
        this.categoryRecipes = this.minecraft.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, i + 25, j + 14, 80, 9 + 5, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(16777215);
        this.searchBox.setValue(s);
        this.forwardButton = new SmithStateSwitchingButton(i + 93, j + 137, 12, 17, false);
        this.forwardButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        this.backButton = new SmithStateSwitchingButton(i + 38, j + 137, 12, 17, true);
        this.backButton.initTextureValues(1, 182, 13, 18, CRAFTING_TABLE_LOCATION);
        tabButtons.clear();
        this.tabButtons.add(new CategoryButton(BteRecipeCategory.ALL));
        this.currentRecipe=null;
        refreshButtons();
        updateTabs();
        this.craftButton = new Button(i + 250, j + 60, 65, 20, Component.literal("Craft"), p_93751_ -> {
            if(this.currentRecipe!=null){
                Entity entity=Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
                if(entity instanceof DruidEntity e){
                    ItemStack stack=this.menu.assemble(this.currentRecipe);
                    e.startCrafting(stack);
                    BteMobsMod.sendToServer(new StartCraftingItemPacket(stack,e.getId()));
                }

                Minecraft.getInstance().setScreen(null);
            }
        });
        this.craftButton.active = false;
    }

    private void refreshButtons(){
        int i = (this.width - 147) / 2 - 86;
        int j = (this.height - 166) / 2;
        if(page < 0) page = 0;
        if(page * 20 > categoryRecipes.size()) page -= 1;
        categoryRecipes =new ArrayList<>( this.minecraft.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get()));
        removeLockedRecipes();
        if(!searchBox.getValue().isEmpty()){
            filterRecipes();
        }
        this.buttonList.clear();
        for(int index = page * 20; index < (page+1) * 20; ++index) {
            if(index >= categoryRecipes.size()) break;
            DruidRecipe recipe = categoryRecipes.get(index);
            ExplorerTableRecipeButton button = new ExplorerTableRecipeButton(i+11, j+31, recipe);
            int moduloIndex = index % 20;
            button.setPosition(i + 6 + 25 * (moduloIndex % 5), j + 31 + 25 * (moduloIndex / 5));
            buttonList.add(button);
            button.setHasEnough(recipe.hasItems(this.minecraft.player));
        }
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
        /*List<DruidRecipe> toRemove = new ArrayList<>();
        for(DruidRecipe recipe : categoryRecipes){
            if(!ClientRecipeLocker.get().hasRecipe(recipe.getResultItem())) {
                toRemove.add(recipe);
            }
        }
        for (BlackSmithRecipe recipe : toRemove){
            categoryRecipes.remove(recipe);
        }*/
    }

    private void filterRecipes(){
        List<DruidRecipe> toRemove = new ArrayList<>();
        for(DruidRecipe recipe : categoryRecipes){
            if(!recipe.getResultItem().getDisplayName().getString().toLowerCase().contains(this.searchBox.getValue().toLowerCase())) {
                toRemove.add(recipe);
            }
        }
        for (DruidRecipe recipe : toRemove){
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
            drawString(p_97795_, this.minecraft.font, "Search recipe", i + 25, j + 14, -1);
        } else {
            this.searchBox.render(p_97795_, p_97796_, p_97797_, p_97798_);
        }
        hoveredButton = null;
        for(ExplorerTableRecipeButton button : buttonList) {
            button.render(p_97795_, p_97796_, p_97797_, p_97798_);
            if(button.isHoveredOrFocused()){
                hoveredButton = button;
            }
        }

        this.backButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        this.forwardButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
        if(previousSearchText != searchBox.getValue()){
            previousSearchText = searchBox.getValue();
            page = 0;
            refreshButtons();
        }

        renderTooltip(p_97795_, p_97796_, p_97797_);
        this.craftButton.render(p_97795_, p_97796_, p_97797_, p_97798_);
    }

    public void renderTooltip(PoseStack p_100418_, int p_100419_, int p_100420_) {
        if (this.minecraft.screen != null && this.hoveredButton != null) {
            this.minecraft.screen.renderComponentTooltip(p_100418_, this.hoveredButton.getTooltipText(this.minecraft.screen), p_100419_, p_100420_, this.hoveredButton.getRecipeStack());
        }

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
        this.font.draw(p_97808_, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
        this.font.draw(p_97808_, this.playerInventoryTitle, (float)this.inventoryLabelX, (float)this.inventoryLabelY, 4210752);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean flag=false;
        for (DruidRecipe recipe : this.categoryRecipes){
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
        Entity entity = Minecraft.getInstance().level.getEntity(this.menu.getEntityId());
        return entity instanceof DruidEntity explorer && !explorer.isCrafting() && entity.isAlive();
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
        for(ExplorerTableRecipeButton button : buttonList) {
            if (button.mouseClicked(p_97748_, p_97749_, p_97750_) && button.hasEnough && this.hasExplorerFree()) {
                BteMobsMod.sendToServer(new PlaceItemRecipePacket(button.getRecipe().getResultItem()));
                this.menu.placeRecipe(this.minecraft.player,button.getRecipe().getResultItem());
                this.currentRecipe= (DruidRecipe) button.getRecipe();
                this.craftButton.active = true;
            }
        }
        return super.mouseClicked(p_97748_, p_97749_, p_97750_);
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
