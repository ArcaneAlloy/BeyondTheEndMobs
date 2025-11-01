package fr.shoqapik.btemobs.integration.warlock_potion_jei;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.integration.JEIPlugin;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
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

import static fr.shoqapik.btemobs.BteMobsMod.getPartialDrawable;

public class WarlockPotionCategory implements IRecipeCategory<WarlockPotionRecipe> {
    public final static ResourceLocation UID = new ResourceLocation(BteMobsMod.MODID, "warlock_recipe");
    public final static ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/cauldron_screen_1.png");

    private final IDrawable background;
    private final IDrawable icon;

    public WarlockPotionCategory(IGuiHelper helper){
        this.background = getPartialDrawable(helper,TEXTURE);

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.POTION));
    }

    @Override
    public RecipeType<WarlockPotionRecipe> getRecipeType() {
        return JEIPlugin.WARLOCK_POTION_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Perdestal");
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
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, WarlockPotionRecipe explorerRecipe, IFocusGroup iFocusGroup) {
        List<Ingredient> ingredients = explorerRecipe.getIngredients();
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 80, 44)
                .addIngredients(Ingredient.of(explorerRecipe.getIngredientPrimary()));
        ItemStack output = explorerRecipe.getResultItem();
        if (!output.isEmpty()) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 154, 35).addItemStack(output);
        } else {
            System.out.println("⚠️ Advertencia: La receta " + explorerRecipe.getId() + " tiene un resultado vacío.");
        }
    }
}
