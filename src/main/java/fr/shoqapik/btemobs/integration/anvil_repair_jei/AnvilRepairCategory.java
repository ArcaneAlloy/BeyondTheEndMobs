package fr.shoqapik.btemobs.integration.anvil_repair_jei;

import fr.shoqapik.btemobs.BteMobsMod;
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

public class AnvilRepairCategory implements IRecipeCategory<AnvilRepairRecipe> {

    public static final ResourceLocation UID =
            new ResourceLocation(BteMobsMod.MODID, "anvil_repair");

    // Same texture used by BlacksmithRepairScreen (extends ItemCombinerScreen)
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_repair.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilRepairCategory(IGuiHelper helper) {
        // ItemCombinerScreen uses 176x166 — crop only the top part with the slots,
        // excluding the player inventory rows (saves vertical space in JEI).
        // The craft area ends at approximately y=68; we show 176x70.
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 70);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.ANVIL));
    }

    @Override
    public RecipeType<AnvilRepairRecipe> getRecipeType() {
        return JEIAnvilRepairPlugin.ANVIL_REPAIR_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Anvil Repair");
    }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public IDrawable getIcon() { return icon; }

    /**
     * Slot positions match ItemCombinerScreen exactly:
     *   slot 0 (base item)     : x=27, y=47
     *   slot 1 (repair material): x=76, y=47
     *   slot 2 (output)        : x=134, y=47
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          AnvilRepairRecipe recipe,
                          IFocusGroup focuses) {

        // Input: the item to repair (slot 0)
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 47)
               .addIngredients(Ingredient.of(recipe.getBase()));

        // Input: repair material (slot 1)
        builder.addSlot(RecipeIngredientRole.INPUT, 76, 47)
               .addIngredients(Ingredient.of(recipe.getRepairMaterial()));

        // Output: repaired item (slot 2)
        ItemStack output = recipe.getBase().copy();
        if (!output.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 47)
                   .addItemStack(output);
        } else {
            System.out.println("⚠️ AnvilRepair: resultado vacío para " + recipe.getId());
        }
    }
}
