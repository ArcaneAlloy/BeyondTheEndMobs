package fr.shoqapik.btemobs.client;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Waypoint temporal de Xaero's Minimap (y por tanto también en Xaero's World Map) que marca a dónde tiene que ir el
 * jugador en el tutorial. Todo por reflexión: no hay dependencia del mod y, si no está o cambia su API, no hace nada.
 *
 * Se añade al conjunto de waypoints actual como "temporary" (Xaero no los guarda en disco) y se quita al cambiar de
 * paso, al acabar el tutorial o al salir del mundo.
 */
final class XaeroWaypoints {
    private XaeroWaypoints() {}

    private static boolean looked = false;
    private static boolean available = false;

    private static Object minimapModule;           // BuiltInHudModules.MINIMAP
    private static Method getCurrentSession;       // HudModule.getCurrentSession()
    private static Method getWorldManager;         // MinimapSession.getWorldManager()
    private static Method getCurrentWorld;         // MinimapWorldManager.getCurrentWorld()
    private static Method getCurrentWaypointSet;   // MinimapWorld.getCurrentWaypointSet()
    private static Method setAdd;                  // WaypointSet.add(Waypoint)
    private static Method setRemove;               // WaypointSet.remove(Waypoint)
    private static Constructor<?> waypointCtor;    // Waypoint(int, int, int, String, String, WaypointColor, WaypointPurpose, boolean, boolean)
    private static Class<?> colorClass;
    private static Object purposeNormal;

    /** Waypoint puesto ahora mismo (y el conjunto en el que está). */
    private static Object current;
    private static Object currentSet;
    private static String currentName;
    private static int curX, curY, curZ;

    private static boolean lookup() {
        if (looked) return available;
        looked = true;
        try {
            if (!ModList.get().isLoaded("xaerominimap")) return false;
            Class<?> modules = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            minimapModule = modules.getField("MINIMAP").get(null);
            getCurrentSession = minimapModule.getClass().getMethod("getCurrentSession");
            Class<?> session = Class.forName("xaero.hud.minimap.module.MinimapSession");
            getWorldManager = session.getMethod("getWorldManager");
            Class<?> worldManager = Class.forName("xaero.hud.minimap.world.MinimapWorldManager");
            getCurrentWorld = worldManager.getMethod("getCurrentWorld");
            Class<?> world = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            getCurrentWaypointSet = world.getMethod("getCurrentWaypointSet");
            Class<?> waypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Class<?> set = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
            setAdd = set.getMethod("add", waypoint);
            setRemove = set.getMethod("remove", waypoint);
            colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            Class<?> purpose = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
            purposeNormal = purpose.getField("NORMAL").get(null);
            waypointCtor = waypoint.getConstructor(int.class, int.class, int.class, String.class, String.class,
                    colorClass, purpose, boolean.class, boolean.class);
            available = true;
        } catch (Throwable t) {
            available = false;
        }
        return available;
    }

    /** Pone (o mueve) el waypoint del tutorial. color: nombre de WaypointColor (GOLD, AQUA, PURPLE...). */
    static void show(String name, int x, int y, int z, String color) {
        if (!lookup()) return;
        if (current != null && name.equals(currentName)
                && Math.abs(x - curX) <= 1 && Math.abs(y - curY) <= 2 && Math.abs(z - curZ) <= 1) return;
        clear();
        try {
            Object session = getCurrentSession.invoke(minimapModule);
            if (session == null) return;
            Object world = getCurrentWorld.invoke(getWorldManager.invoke(session));
            if (world == null) return;
            Object set = getCurrentWaypointSet.invoke(world);
            if (set == null) return;
            Object waypointColor = colorClass.getField(color).get(null);
            // Temporal (no se guarda) y con la altura incluida, para que el marcador quede a la altura del sitio
            Object waypoint = waypointCtor.newInstance(x, y, z, name, "!", waypointColor, purposeNormal, true, true);
            setAdd.invoke(set, waypoint);
            current = waypoint;
            currentSet = set;
            currentName = name;
            curX = x;
            curY = y;
            curZ = z;
        } catch (Throwable t) {
            // API distinta: se deja de intentar
            available = false;
            current = null;
            currentSet = null;
        }
    }

    /** Quita el waypoint del tutorial, si hay. */
    static void clear() {
        if (current == null) return;
        try {
            if (currentSet != null) setRemove.invoke(currentSet, current);
        } catch (Throwable ignored) {
        }
        current = null;
        currentSet = null;
        currentName = null;
    }
}
