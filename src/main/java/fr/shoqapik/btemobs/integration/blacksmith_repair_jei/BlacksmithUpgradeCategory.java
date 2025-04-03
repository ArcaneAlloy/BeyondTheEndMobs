package fr.shoqapik.btemobs.integration.blacksmith_repair_jei;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
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

public class BlacksmithUpgradeCategory implements IRecipeCategory<BlacksmithUpgradeRecipe> {
    public final static ResourceLocation UID = new ResourceLocation(BteMobsMod.MODID, "blacksmith_upgrade");
    public final static ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_screen.png");

    private final IDrawable background;
    private final IDrawable icon;

    public BlacksmithUpgradeCategory(IGuiHelper helper){
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 85);

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.DAMAGED_ANVIL));
    }

    @Override
    public RecipeType<BlacksmithUpgradeRecipe> getRecipeType() {
        return JEIBlacksmithUpgradePlugin.BLACKSMITH_UPGRADE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Blacksmith Upgrade Anvil");
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
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, BlacksmithUpgradeRecipe blacksmithRecipe, IFocusGroup iFocusGroup) {

        List<Ingredient> ingredients = blacksmithRecipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 62 + (i % 3) * 18, 20 + (i / 3) * 18)
                    .addIngredients(ingredients.get(i));
        }
        ItemStack output = blacksmithRecipe.getResultItem();
        if (!output.isEmpty()) {
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 117, 30).addItemStack(output);
        } else {
            System.out.println("⚠️ Advertencia: La receta " + blacksmithRecipe.getId() + " tiene un resultado vacío.");
        }

    }
}
