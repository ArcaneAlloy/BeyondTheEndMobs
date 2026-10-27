package fr.shoqapik.btemobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Zona de pantalla que ocupa la informacion del Nature's Compass (Status, Biome, Coordinates, Distance...)
 * mientras el jugador lo lleva en la mano. El panel de quests seguidas se coloca debajo para no solaparse.
 *
 * Nature's Compass 1.10.0 (RenderUtils#drawConfiguredStringOnHUD) dibuja la linea n en
 * y = 5 + (n + overlayLineOffset) * 9 + 2, en el lado overlaySide de su config de cliente. Usa como mucho
 * las lineas 0..7 buscando y 0..10 con el resultado. Su config se lee por reflexion: no es dependencia.
 */
final class NaturesCompassHud {

    private static final ResourceLocation COMPASS_ID = new ResourceLocation("naturescompass", "naturescompass");
    private static final int STATE_SEARCHING = 1;

    private static boolean lookedUp = false;
    private static Object lineOffsetValue; // ForgeConfigSpec.IntValue
    private static Object sideValue;       // ForgeConfigSpec.EnumValue<OverlaySide>
    private static Method configGet;

    private NaturesCompassHud() {}

    /** Primera coordenada Y (pixeles de GUI) libre por debajo del HUD del Nature's Compass; 0 si no se ve. */
    static int bottom(Minecraft mc) {
        if (mc.player == null || !ModList.get().isLoaded("naturescompass")) return 0;

        ItemStack held = compass(mc.player.getMainHandItem());
        if (held == null) held = compass(mc.player.getOffhandItem());
        if (held == null) return 0;

        CompoundTag tag = held.getTag();
        int state = tag != null ? tag.getInt("State") : 0;
        if (state == 0) return 0; // INACTIVE: no dibuja nada

        lookUpConfig();
        if (!"LEFT".equals(String.valueOf(get(sideValue, "LEFT")))) return 0; // a la derecha no molesta
        int offset = ((Number) get(lineOffsetValue, 1)).intValue();
        int lastLine = state == STATE_SEARCHING ? 7 : 10;
        return 5 + (lastLine + 1 + offset) * 9 + 2;
    }

    private static ItemStack compass(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return COMPASS_ID.equals(id) ? stack : null;
    }

    private static void lookUpConfig() {
        if (lookedUp) return;
        lookedUp = true;
        try {
            Class<?> handler = Class.forName("com.chaosthedude.naturescompass.config.ConfigHandler");
            Object client = handler.getField("CLIENT").get(null);
            Field offset = client.getClass().getField("overlayLineOffset");
            Field side = client.getClass().getField("overlaySide");
            lineOffsetValue = offset.get(client);
            sideValue = side.get(client);
            configGet = Class.forName("net.minecraftforge.common.ForgeConfigSpec$ConfigValue").getMethod("get");
        } catch (Throwable ignored) {
            // Otra version de Nature's Compass: se usan los valores por defecto (linea 1, izquierda)
        }
    }

    private static Object get(Object value, Object fallback) {
        if (value == null || configGet == null) return fallback;
        try {
            Object v = configGet.invoke(value);
            return v != null ? v : fallback;
        } catch (Throwable e) {
            return fallback;
        }
    }
}
