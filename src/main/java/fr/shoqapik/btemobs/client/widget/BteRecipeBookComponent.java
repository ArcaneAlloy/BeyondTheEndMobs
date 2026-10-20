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
    // Referencia a la pantalla padre para dibujar su buscador dentro del pushPose/popPose
    public fr.shoqapik.btemobs.client.gui.BteAbstractCraftScreen parentScreen = null;

    /** Mismo hint que el resto de interfaces de crafteo: traducido, en cursiva y gris. */
    private static final net.minecraft.network.chat.Component SEARCH_HINT =
            net.minecraft.network.chat.Component.translatable("gui.recipebook.search_hint")
                    .withStyle(net.minecraft.ChatFormatting.ITALIC)
                    .withStyle(net.minecraft.ChatFormatting.GRAY);

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
            EditBox search = parentScreen.searchBox;
            if (search != null) {
                // Como en las demas interfaces: sin foco y vacio se ve el hint; si no, el EditBox real
                // (con su cursor parpadeante, la seleccion y el desplazamiento del texto largo).
                if (!search.isFocused() && search.getValue().isEmpty()) {
                    net.minecraft.client.gui.GuiComponent.drawString(pPoseStack,
                        net.minecraft.client.Minecraft.getInstance().font,
                        SEARCH_HINT, search.x, search.y, -1);
                } else {
                    search.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                }
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
