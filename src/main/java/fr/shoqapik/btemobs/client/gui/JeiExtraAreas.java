package fr.shoqapik.btemobs.client.gui;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * Zonas de una pantalla que quedan fuera de su imageWidth/imageHeight (panel de recetas, pestanas) y que JEI
 * debe evitar para no dibujar encima su lista de items ni los favoritos. Se registran en JEIPlugin.
 */
public interface JeiExtraAreas {
    List<Rect2i> getJeiExtraAreas();
}
