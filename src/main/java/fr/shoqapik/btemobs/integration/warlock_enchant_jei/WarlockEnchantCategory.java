package fr.shoqapik.btemobs.integration.warlock_enchant_jei;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.integration.JEIPlugin;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

import static fr.shoqapik.btemobs.BteMobsMod.getPartialDrawable;

public class WarlockEnchantCategory implements IRecipeCategory<WarlockRecipe> {
    public final static ResourceLocation UID = new ResourceLocation(BteMobsMod.MODID, "warlock_enchant_recipe");
    public final static ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_screen.png");

    private final IDrawable background;
    private final IDrawable icon;

    public WarlockEnchantCategory(IGuiHelper helper){
        this.background = getPartialDrawable(helper,TEXTURE);

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.ENCHANTED_BOOK));
    }

    @Override
    public RecipeType<WarlockRecipe> getRecipeType() {
        return JEIPlugin.WARLOCK_ENCHANT_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Warlock");
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
    public void draw(WarlockRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, stack, mouseX, mouseY);
        Component xp = Component.translatable("gui.jei.category.warlock_enchant.exp").append(" "+recipe.getExperience());
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(stack, xp, 81.0F, 60.0F, 8453920);
        Component eyes = Component.translatable("gui.jei.category.warlock_enchant.eyes").append(" "+recipe.getNeedEyes());
        minecraft.font.draw(stack, eyes, 81.0F, 70.0F, 8453920);

    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, WarlockRecipe explorerRecipe, IFocusGroup iFocusGroup) {
        List<Ingredient> ingredients = explorerRecipe.getIngredients();
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 42 , 7)
                .addIngredients(ingredients.get(0));
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 29 , 33)
                .addIngredients(ingredients.get(1));
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 42 , 59)
                .addIngredients(ingredients.get(2));

        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 81 , 33)
                .addIngredients(Ingredient.of(new ItemStack(Items.BOOK)));

        ItemStack output = explorerRecipe.getResultItem();
        if (!output.isEmpty()) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 133, 31).addItemStack(output);
        } else {
            System.out.println("⚠️ Advertencia: La receta " + explorerRecipe.getId() + " tiene un resultado vacío.");
        }
    }
}
