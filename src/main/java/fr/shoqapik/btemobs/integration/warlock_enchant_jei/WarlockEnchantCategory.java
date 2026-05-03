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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

import static fr.shoqapik.btemobs.BteMobsMod.getPartialDrawable;

public class WarlockEnchantCategory implements IRecipeCategory<WarlockRecipe> {
    public final static ResourceLocation UID = new ResourceLocation(BteMobsMod.MODID, "warlock_enchant_recipe");
    public final static ResourceLocation TEXTURE =
            new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/warlock_screen.png");

    private final IDrawable background;
    private final IDrawable icon;
    private int[][] slots = {
        {42,29,42},
        {7,33,59}
    };

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
        Component eyes = Component.translatable("gui.jei.category.warlock_enchant.eyes").append(" "+recipe.getNeedEyes()).withStyle(ChatFormatting.RED);
        minecraft.font.draw(stack, eyes, 81.0F, 70.0F, 8453920);

    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, WarlockRecipe explorerRecipe, IFocusGroup iFocusGroup) {
        List<Ingredient> ingredients = explorerRecipe.getIngredients();
        for(int i = 0 ; i<ingredients.size() ; i++){
            iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, slots[0][i] , slots[1][i])
                    .addIngredients(ingredients.get(i));
        }
        ItemStack output = explorerRecipe.getResultItem();

        if (!output.isEmpty()) {
            net.minecraft.world.item.enchantment.Enchantment enchantment = explorerRecipe.getEnchantment();

            boolean isAboveMaxLevel = explorerRecipe.getLevel() > enchantment.getMaxLevel();

            if (isAboveMaxLevel) {
                // Nivel superior al maximo vanilla - resultado es Ancient Tome de Quark
                net.minecraft.world.item.Item ancientTomeItem =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        new net.minecraft.resources.ResourceLocation("quark:ancient_tome"));

                if (ancientTomeItem != null) {
                    // Input: libro encantado del nivel anterior (base del Ancient Tome)
                    ItemStack prevBook = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, explorerRecipe.getLevel() - 1));
                    iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 81, 33)
                            .addItemStack(prevBook);

                    // Output: Ancient Tome con el encantamiento
                    ItemStack ancientTome = new ItemStack(ancientTomeItem);
                    net.minecraft.world.item.EnchantedBookItem.addEnchantment(ancientTome,
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, explorerRecipe.getLevel()));
                    iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 133, 33)
                            .addItemStack(ancientTome);

                    // Invisible para U en el ancient tome
                    iRecipeLayoutBuilder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                            .addItemStack(ancientTome);
                }
            } else {
                // Nivel normal - resultado es item encantado o libro encantado
                List<ItemStack> sinEncantar = new ArrayList<>();
                List<ItemStack> encantados = new ArrayList<>();
                for (net.minecraft.world.item.Item item : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()) {
                    ItemStack stack = new ItemStack(item);
                    if (enchantment.canEnchant(stack)) {
                        sinEncantar.add(stack.copy());
                        ItemStack enchanted = stack.copy();
                        enchanted.enchant(enchantment, explorerRecipe.getLevel());
                        encantados.add(enchanted);
                    }
                }

                iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 81, 33)
                        .addItemStacks(sinEncantar);
                iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 133, 33)
                        .addItemStacks(encantados);

                // Book como INPUT invisible: permite que U en book encuentre esta receta
                iRecipeLayoutBuilder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                        .addItemStack(new ItemStack(Items.BOOK));

                // Libro encantado como OUTPUT invisible
                iRecipeLayoutBuilder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                        .addItemStack(output);
            }
        } else {
            System.out.println("⚠️ Advertencia: La receta " + explorerRecipe.getId() + " tiene un resultado vacío.");
        }
    }
}
