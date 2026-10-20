package fr.shoqapik.btemobs.mixin;

import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Las pestanas del recipe book del Blacksmith se registran al arrancar con un icono fijo, cuando los items de otros
 * mods pueden no existir todavia. Para las categorias con icono de otro mod (BteRecipeCategory#hasCustomIcon, p. ej.
 * Artifacts con dungeons_gear:iron_hide_amulet) devolvemos ese icono al dibujar la pestana.
 */
@Mixin(RecipeBookCategories.class)
public class RecipeBookCategoriesIconMixin {

    @Inject(method = "getIconItems", at = @At("HEAD"), cancellable = true)
    private void btemobs$customIcon(CallbackInfoReturnable<List<ItemStack>> cir) {
        String name = ((Enum<?>) (Object) this).name();
        for (BteRecipeCategory category : BteRecipeCategory.values()) {
            if (!category.hasCustomIcon()) continue;
            for (BteNpcType npc : category.npcs) {
                if (name.equals(npc.name() + "_" + category.name())) {
                    cir.setReturnValue(List.of(category.getIcon()));
                    return;
                }
            }
        }
    }
}
