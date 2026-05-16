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

    // Modified version of blacksmith_repair_jei.png with the red EditBox area painted over
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/blacksmith_repair_jei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilRepairCategory(IGuiHelper helper) {
        // Crop 176x70: shows the hammer decoration and the 3 slots, no inventory rows
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
     *   slot 0 (damaged item)   : x=27, y=47
     *   slot 1 (repair material): x=76, y=47
     *   slot 2 (output)         : x=134, y=47
     *
     * The input item is shown at ~75% damage to communicate to the player
     * that this recipe repairs the item, not crafts it.
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          AnvilRepairRecipe recipe,
                          IFocusGroup focuses) {

        // Input slot 0: the item shown as damaged (75% of max durability used)
        ItemStack damagedInput = recipe.getBase().copy();
        int maxDamage = damagedInput.getMaxDamage();
        if (maxDamage > 0) {
            damagedInput.setDamageValue((int) (maxDamage * 0.75));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 47)
               .addItemStack(damagedInput);

        // Input slot 1: repair material
        builder.addSlot(RecipeIngredientRole.INPUT, 76, 47)
               .addIngredients(Ingredient.of(recipe.getRepairMaterial()));

        // Output slot 2: the item fully repaired
        ItemStack repairedOutput = recipe.getBase().copy();
        // Ensure output shows full durability (damage = 0)
        repairedOutput.setDamageValue(0);
        if (!repairedOutput.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 47)
                   .addItemStack(repairedOutput);
        } else {
            System.out.println("⚠️ AnvilRepair: resultado vacío para " + recipe.getId());
        }
    }
}
