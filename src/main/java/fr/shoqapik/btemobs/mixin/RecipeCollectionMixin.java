package fr.shoqapik.btemobs.mixin;

import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(RecipeCollection.class)
public class RecipeCollectionMixin {

    /*@Shadow @Final
    private Set<Recipe<?>> craftable;

    @Shadow @Final private List<Recipe<?>> recipes;

    @Inject(method = "canCraft", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    public void canCraftAdd(StackedContents pHandler, int pWidth, int pHeight, RecipeBook pBook, CallbackInfo ci, Iterator<Recipe<?>> var5, Recipe<?> recipe, boolean $$5) {
        if(recipe instanceof BteAbstractRecipe) {
            if(!((BteAbstractRecipe) recipe).hasItems(Minecraft.getInstance().player)) {
                this.craftable.remove(recipe);
            }
        }
        if(recipe instanceof UpgradeRecipe) {
            if(!hasItemsSmithing((UpgradeRecipe) recipe, Minecraft.getInstance().player)) {
                this.craftable.remove(recipe);
            }
        }
    }

    @Inject(method = "canCraft", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    public void canCraftRemove(StackedContents pHandler, int pWidth, int pHeight, RecipeBook pBook, CallbackInfo ci, Iterator<Recipe<?>> var5, Recipe<?> recipe) {
        if(recipe instanceof BteAbstractRecipe) {
            if(((BteAbstractRecipe) recipe).hasItems(Minecraft.getInstance().player)) {
                this.craftable.add(recipe);
            }
        }
        if(recipe instanceof UpgradeRecipe) {
            if(hasItemsSmithing((UpgradeRecipe) recipe, Minecraft.getInstance().player)) {
                this.craftable.add(recipe);
            }
        }
    }

    public boolean hasItemsSmithing(UpgradeRecipe recipe, Player player) {
        boolean base = false;
        boolean addition = false;

        for(ItemStack itemStack : player.getInventory().items) {
            if(recipe.base.test(itemStack)) base = true;
            if(recipe.isAdditionIngredient(itemStack)) addition = true;
            if(base && addition) return true;
        }

        return false;
    }

     */
}
