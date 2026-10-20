package fr.shoqapik.btemobs.mixin;

import fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * En la mesa de Anna (recipe book vanilla) las recetas que se pueden craftear salen primero, como en el resto de
 * interfaces de crafteo de los NPCs. El orden es estable: dentro de cada grupo se mantiene el original.
 * Solo afecta a las pantallas BteAbstractCraftScreen; el recipe book vanilla no cambia.
 */
@Mixin(RecipeBookPage.class)
public class RecipeBookPageMixin {

    @ModifyVariable(method = "updateCollections", at = @At("HEAD"), argsOnly = true)
    private List<RecipeCollection> btemobs$craftableFirst(List<RecipeCollection> collections) {
        if (!(Minecraft.getInstance().screen instanceof BteAbstractCraftScreen)) return collections;
        List<RecipeCollection> sorted = new ArrayList<>(collections);
        sorted.sort((a, b) -> Boolean.compare(b.hasCraftable(), a.hasCraftable()));
        return sorted;
    }
}
