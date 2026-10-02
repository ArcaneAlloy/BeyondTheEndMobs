package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.capability.RecipeCapability;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Recetas compartidas (modo cooperativo).
 *
 * El estado de desbloqueo de cada receta vive en ServerData (uno por mundo). Cuando se desbloquea una receta
 * (por ingrediente, por quest o con un libro), se marca ahí y se entrega a todos los jugadores conectados.
 * Al conectarse, cada jugador recibe todas las recetas ya desbloqueadas.
 *
 *  - Recetas de Anna (blacksmith / blacksmith_upgrade): libro de recetas vanilla de cada jugador.
 *  - Resto de NPC: lista de recetas del capability del jugador.
 */
public final class RecipeSharing {

    private RecipeSharing() {}

    private static boolean usesRecipeBook(Recipe<?> recipe) {
        String t = recipe.getType().toString();
        return t.equals("blacksmith") || t.equals("blacksmith_upgrade");
    }

    /** Marca las recetas como desbloqueadas para todo el mundo y se las da a todos los jugadores conectados. */
    public static void unlockForEveryone(Collection<? extends Recipe<?>> recipes) {
        if (recipes.isEmpty()) return;
        ServerData data = ServerData.get();
        for (Recipe<?> recipe : recipes) data.markUnlocked(recipe);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            give(player, recipes);
        }
    }

    /** Da al jugador todas las recetas desbloqueadas en el mundo (al conectarse). */
    public static void syncPlayer(ServerPlayer player) {
        RecipeManager manager = player.getServer().getRecipeManager();
        List<Recipe<?>> unlocked = new ArrayList<>();
        for (Map.Entry<RecipeType<?>, List<UnlockRecipe>> entry : ServerData.get().getRecipesManager().entrySet()) {
            for (UnlockRecipe u : entry.getValue()) {
                if (u.recipe == null || u.isLock || !u.wasFound) continue;
                // Tras un /reload el objeto guardado puede ser antiguo: se busca el actual por id
                manager.byKey(u.recipe.getId()).ifPresent(unlocked::add);
            }
        }
        give(player, unlocked);
    }

    /**
     * Comparte con todos las recetas que este jugador ya tenía (partidas anteriores al modo cooperativo,
     * o recetas del libro de Anna desbloqueadas antes).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void shareFromPlayer(ServerPlayer player) {
        List<Recipe<?>> mine = new ArrayList<>();
        RecipeCapability cap = RecipeCapability.get(player);
        if (cap != null) {
            for (Object list : cap.getRecipeManager().values()) {
                if (list != null) mine.addAll((List<Recipe<?>>) list);
            }
        }
        RecipeManager manager = player.getServer().getRecipeManager();
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (usesRecipeBook(recipe) && player.getRecipeBook().contains(recipe)) mine.add(recipe);
        }
        ServerData data = ServerData.get();
        List<Recipe<?>> fresh = new ArrayList<>();
        for (Recipe<?> recipe : mine) {
            if (!data.isUnlock(recipe)) fresh.add(recipe);
        }
        unlockForEveryone(fresh);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void give(ServerPlayer player, Collection<? extends Recipe<?>> recipes) {
        List<Recipe<?>> toBook = new ArrayList<>();
        RecipeCapability cap = RecipeCapability.get(player);
        for (Recipe<?> recipe : recipes) {
            if (usesRecipeBook(recipe)) {
                if (!player.getRecipeBook().contains(recipe)) toBook.add(recipe);
            } else if (cap != null) {
                cap.addRecipeForType((RecipeType) recipe.getType(), recipe);
            }
        }
        if (!toBook.isEmpty()) player.awardRecipes(toBook);
    }
}
