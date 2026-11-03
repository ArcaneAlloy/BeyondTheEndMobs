package fr.shoqapik.btemobs.integration.explorer_craft_jei;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.integration.JEIPlugin;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

public class ExplorerCraftCategory implements IRecipeCategory<ExplorerRecipe> {
    public final static ResourceLocation UID = new ResourceLocation(BteMobsMod.MODID, "explorer_recipe");
    public final static ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ExplorerCraftCategory(IGuiHelper helper){
        this.background = fr.shoqapik.btemobs.integration.QuestLockStrip.extend(BteMobsMod.getPartialDrawable(helper,TEXTURE));

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(BteMobsBlocks.EXPLORER_TABLE.get()));
    }

    @Override
    public RecipeType<ExplorerRecipe> getRecipeType() {
        return JEIExplorerCraftPlugin.EXPLORER_CRAFT_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Explorer Table");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, ExplorerRecipe explorerRecipe, IFocusGroup iFocusGroup) {

        List<Ingredient> ingredients = explorerRecipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 62 + (i % 3) * 18, 20 + (i / 3) * 18)
                    .addIngredients(ingredients.get(i));
        }
        ItemStack output = explorerRecipe.getResultItem();
        if (!output.isEmpty()) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 117, 30).addItemStack(output);
        } else {
            System.out.println("⚠️ Advertencia: La receta " + explorerRecipe.getId() + " tiene un resultado vacío.");
        }

    }

    // ── Aviso de receta bloqueada por una quest (franja "Requiere:" debajo de la receta) ──

    @javax.annotation.Nullable
    private static String lockQuest(ExplorerRecipe recipe) {
        return fr.shoqapik.btemobs.integration.QuestLockStrip.questForRecipe(recipe.getId());
    }

    @Override
    public void draw(ExplorerRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     com.mojang.blaze3d.vertex.PoseStack poseStack, double mouseX, double mouseY) {
        fr.shoqapik.btemobs.integration.QuestLockStrip.draw(poseStack, lockQuest(recipe), this.background.getWidth(),
                this.background.getHeight() - fr.shoqapik.btemobs.integration.QuestLockStrip.EXTRA_HEIGHT, mouseX, mouseY);
    }

    @Override
    public boolean handleInput(ExplorerRecipe recipe, double mouseX, double mouseY,
                               com.mojang.blaze3d.platform.InputConstants.Key input) {
        return fr.shoqapik.btemobs.integration.QuestLockStrip.handleInput(lockQuest(recipe), this.background.getWidth(),
                this.background.getHeight() - fr.shoqapik.btemobs.integration.QuestLockStrip.EXTRA_HEIGHT, mouseX, mouseY, input);
    }
}
