package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;

import java.lang.reflect.Field;
import java.util.List;

public class BteRecipeBookComponent extends RecipeBookComponent {

    private static Field internalBoxField = null;

    public static EditBox getInternalBox(RecipeBookComponent instance) {
        try {
            if (internalBoxField == null) {
                internalBoxField = RecipeBookComponent.class.getDeclaredField("f_100281_");
                internalBoxField.setAccessible(true);
            }
            return (EditBox) internalBoxField.get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Override render() para suprimir el hint "Search..." del vanilla.
     * RecipeBookComponent.render() dibuja el hint cuando box.getValue().isEmpty().
     * Ponemos " " antes y lo restauramos despues para no afectar el filtrado.
     */
    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        EditBox box = getInternalBox(this);
        int savedX = -1, savedY = -1;
        String savedValue = null;
        if (box != null) {
            // Mover fuera de pantalla para que el box no se renderice
            savedX = box.x;
            savedY = box.y;
            box.x = -10000;
            box.y = -10000;
            // Poner " " para que isEmpty()==false y vanilla no dibuje el hint
            savedValue = box.getValue();
            if (savedValue.isEmpty()) {
                box.setValue(" ");
            }
        }
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // Restaurar estado original
        if (box != null && savedX != -1) {
            box.x = savedX;
            box.y = savedY;
            if (savedValue != null && savedValue.isEmpty()) {
                box.setValue(savedValue);
            }
        }
    }

    @Override
    public void setupGhostRecipe(Recipe<?> pRecipe, List<Slot> pSlots) {
        this.ghostRecipe.setRecipe(pRecipe);
        if (pRecipe instanceof UpgradeRecipe) {
            UpgradeRecipe recipe = (UpgradeRecipe) pRecipe;
            this.ghostRecipe.addIngredient(recipe.base, (pSlots.get(0)).x, (pSlots.get(0)).y);
            this.ghostRecipe.addIngredient(recipe.addition, (pSlots.get(1)).x, (pSlots.get(1)).y);
        }
        for (int i = 0; i < pRecipe.getIngredients().size(); i++) {
            if (this.menu.getSlot(i).getItem().isEmpty()) {
                this.ghostRecipe.addIngredient(pRecipe.getIngredients().get(i), pSlots.get(i).x, pSlots.get(i).y);
            }
        }
    }
}
