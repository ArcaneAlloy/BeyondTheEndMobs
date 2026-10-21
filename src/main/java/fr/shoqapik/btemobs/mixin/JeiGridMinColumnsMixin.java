package fr.shoqapik.btemobs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * JEI solo muestra los favoritos (y la lista de items) si caben al menos 4 columnas, y ese minimo no se puede cambiar
 * en su config. Con escalas de interfaz grandes, el hueco libre a la izquierda de las interfaces de crafteo de los
 * NPCs solo tiene sitio para 2 o 3 columnas y los favoritos desaparecian. Bajamos el minimo a 2 columnas: JEI sigue
 * usando todas las que quepan (hasta su maximo), solo cambia que ya no oculta la lista en huecos estrechos.
 * Clase de JEI 11.8.x (la del modpack). Sin JEI (o si cambia la clase) no se aplica.
 */
@Pseudo
@Mixin(targets = "mezz.jei.common.config.IngredientGridConfig", remap = false)
public class JeiGridMinColumnsMixin {

    private static final int BTEMOBS_MIN_COLUMNS = 2;

    @Inject(method = "getMinColumns", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void btemobs$fewerMinColumns(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BTEMOBS_MIN_COLUMNS);
    }
}
