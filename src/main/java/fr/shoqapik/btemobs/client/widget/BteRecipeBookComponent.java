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
    // Referencia a la pantalla padre para dibujar el hint dentro del pushPose/popPose
    public fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen parentScreen = null;

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        EditBox box = getInternalBox(this);
        int savedX = -1, savedY = -1;
        String savedValue = null;
        if (box != null) {
            savedX = box.x;
            savedY = box.y;
            // Mover fuera de pantalla para que vanilla no dibuje ni el hint ni el texto
            box.x = -10000;
            box.y = -10000;
            savedValue = box.getValue();
            if (savedValue.isEmpty()) {
                box.setValue(" ");
            }
        }
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        // super.render() hace pushPose()+translate(0,0,100) al inicio y popPose() al final.
        // Ahora el PoseStack esta restaurado pero necesitamos dibujar dentro de la misma
        // capa Z. Lo hacemos con nuestro propio pushPose+translate.
        if (parentScreen != null) {
            pPoseStack.pushPose();
            pPoseStack.translate(0, 0, 100);
            String val = parentScreen.searchBox != null ? parentScreen.searchBox.getValue() : "";
            int hx = savedX > -1 ? savedX + 2 : 0;
            int hy = savedY > -1 ? savedY + 2 : 0;
            if (val.isEmpty()) {
                net.minecraft.client.gui.GuiComponent.drawString(pPoseStack,
                    net.minecraft.client.Minecraft.getInstance().font,
                    "Search...", hx, hy, 0xFF808080);
            } else {
                net.minecraft.client.gui.GuiComponent.drawString(pPoseStack,
                    net.minecraft.client.Minecraft.getInstance().font,
                    val, hx, hy, 0xFFFFFF);
            }
            pPoseStack.popPose();
        }
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
