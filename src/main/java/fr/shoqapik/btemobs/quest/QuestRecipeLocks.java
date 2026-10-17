package fr.shoqapik.btemobs.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Recetas bloqueadas por quests.
 *
 * Una receta que es recompensa de alguna quest (UNLOCK_RECIPE o UNLOCK_RECIPES_BY_INGREDIENT) NO se desbloquea
 * sola al tener un ingrediente en el inventario: solo al completar esa quest.
 *
 *  - UNLOCK_RECIPE: todas las recetas del recipeType cuyo resultado es el item (o un item del tag "#mod:tag").
 *                   Incluye todas las variantes (p. ej. las 16 recetas de Netherite Boots).
 *  - UNLOCK_RECIPES_BY_INGREDIENT: todas las recetas de los NPC que usan ese ingrediente, EXCEPTO las que ya
 *                   desbloquea otra quest con UNLOCK_RECIPE (p. ej. Husk Hammer).
 *
 * Funciona igual en servidor (RecipeManager del servidor + QuestManager) y en cliente
 * (RecipeManager sincronizado + quests del capability) para mostrar las listas en la interfaz.
 */
public final class QuestRecipeLocks {

    private QuestRecipeLocks() {}

    /** Tipos de receta de los NPC que se revisan en los desbloqueos por ingrediente. */
    private static final List<String> NPC_RECIPE_TYPES = List.of(
            "blacksmith", "blacksmith_upgrade", "druid_recipe_type", "warlock_potion", "explorer_recipe_type");

    private static RecipeManager cachedManager;
    private static Collection<Quest> cachedQuests;
    private static Set<ResourceLocation> cachedLocked;

    private static RecipeManager inheritedManager;
    private static Collection<Quest> inheritedQuests;
    private static Map<ResourceLocation, Quest> cachedInherited;

    /** Invalida la caché (al recargar quests o recetas). */
    public static void invalidate() {
        cachedManager = null;
        cachedQuests = null;
        cachedLocked = null;
        inheritedManager = null;
        inheritedQuests = null;
        cachedInherited = null;
    }

    /** Recetas del Blacksmith que heredan el bloqueo de su base / ingredientes. */
    private static final List<String> BLACKSMITH_RECIPE_TYPES = List.of("blacksmith", "blacksmith_upgrade");

    /**
     * Bloqueos heredados: recetas del Blacksmith que no son recompensa de ninguna quest, pero que usan como base o
     * ingrediente un objeto que SOLO se fabrica con recetas bloqueadas por una quest (p. ej. la Cocktail Cutlass sale
     * de la Diamond Sword, que bloquea Carminite Giant Block). Quedan bloqueadas por esa misma quest.
     *
     *  - Funciona en cadena (Gauntlet of Maelstrom <- Gauntlet of Bulwark <- Bulwark of the Flame <- Netherite Shield).
     *  - Si el objeto se puede fabricar con alguna receta no bloqueada, de cualquier tipo (mesa de crafteo, horno,
     *    otro NPC...), no hereda nada: se puede conseguir sin la quest.
     *  - Se desbloquean al reclamar esa quest (y, en mundos donde ya estaba reclamada, al comprobar las recetas).
     *
     * Devuelve receta -> quest que la bloquea.
     */
    public static Map<ResourceLocation, Quest> inheritedLocks(RecipeManager manager, Collection<Quest> quests) {
        if (cachedInherited != null && inheritedManager == manager && inheritedQuests == quests) return cachedInherited;

        // Receta -> quest, de los bloqueos directos y por ingrediente
        Map<ResourceLocation, Quest> lockedBy = new HashMap<>();
        for (Quest q : quests) {
            for (RewardData r : q.getRewards()) {
                if (!(r instanceof UnlockRecipeRewardData) && !(r instanceof IngredientRecipesRewardData)) continue;
                for (Recipe<?> recipe : recipesFor(manager, quests, r)) lockedBy.putIfAbsent(recipe.getId(), q);
            }
        }

        // Todas las recetas (de cualquier tipo) que fabrican cada objeto
        Map<net.minecraft.world.item.Item, List<Recipe<?>>> producers = new HashMap<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            ItemStack result;
            try {
                result = recipe.getResultItem();
            } catch (Throwable t) {
                continue;
            }
            if (result == null || result.isEmpty()) continue;
            producers.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(recipe);
        }

        Map<ResourceLocation, Quest> inherited = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Recipe<?> recipe : manager.getRecipes()) {
                if (!BLACKSMITH_RECIPE_TYPES.contains(recipe.getType().toString())) continue;
                if (lockedBy.containsKey(recipe.getId())) continue;
                Quest quest = null;
                for (Ingredient ing : recipe.getIngredients()) {
                    ItemStack[] items = ing.getItems();
                    if (items.length == 0) continue;
                    // El ingrediente solo cuenta si TODAS sus opciones están bloqueadas
                    Quest ingQuest = null;
                    for (ItemStack stack : items) {
                        Quest q = itemLockedBy(stack.getItem(), producers, lockedBy);
                        if (q == null) {
                            ingQuest = null;
                            break;
                        }
                        if (ingQuest == null) ingQuest = q;
                    }
                    if (ingQuest != null) {
                        quest = ingQuest;
                        break;
                    }
                }
                if (quest != null) {
                    lockedBy.put(recipe.getId(), quest);
                    inherited.put(recipe.getId(), quest);
                    changed = true;
                }
            }
        }
        inheritedManager = manager;
        inheritedQuests = quests;
        cachedInherited = inherited;
        return inherited;
    }

    /** Quest que bloquea un objeto, si todas las recetas que lo fabrican están bloqueadas (null si no). */
    private static Quest itemLockedBy(net.minecraft.world.item.Item item,
                                      Map<net.minecraft.world.item.Item, List<Recipe<?>>> producers,
                                      Map<ResourceLocation, Quest> lockedBy) {
        List<Recipe<?>> list = producers.get(item);
        if (list == null || list.isEmpty()) return null;
        Quest quest = null;
        for (Recipe<?> r : list) {
            Quest q = lockedBy.get(r.getId());
            if (q == null) return null;
            if (quest == null) quest = q;
        }
        return quest;
    }

    /** Recetas heredadas que desbloquea reclamar esta quest. */
    public static List<Recipe<?>> inheritedRecipesFor(RecipeManager manager, Collection<Quest> quests, Quest quest) {
        List<Recipe<?>> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Quest> e : inheritedLocks(manager, quests).entrySet()) {
            if (!e.getValue().getId().equals(quest.getId())) continue;
            manager.byKey(e.getKey()).ifPresent(list::add);
        }
        return list;
    }

    public static RecipeType<?> recipeTypeFor(String name) {
        for (RecipeType<?> type : ForgeRegistries.RECIPE_TYPES.getValues()) {
            if (type.toString().equals(name)) return type;
        }
        return null;
    }

    private static boolean resultMatches(Recipe<?> recipe, String spec) {
        ItemStack result = recipe.getResultItem();
        if (result.isEmpty()) return false;
        if (spec.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(spec.substring(1));
            return tag != null && result.is(ItemTags.create(tag));
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(result.getItem());
        return id != null && id.toString().equals(spec);
    }

    private static boolean usesIngredient(Recipe<?> recipe, String itemId) {
        for (Ingredient ing : recipe.getIngredients()) {
            for (ItemStack stack : ing.getItems()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id != null && id.toString().equals(itemId)) return true;
            }
        }
        return false;
    }

    /** Recetas que desbloquea una recompensa UNLOCK_RECIPE (sin tener en cuenta exclusiones). */
    private static List<Recipe<?>> directRecipes(RecipeManager manager, UnlockRecipeRewardData data) {
        List<Recipe<?>> list = new ArrayList<>();
        RecipeType<?> type = recipeTypeFor(data.recipeType);
        if (type == null) return list;
        for (Recipe<?> r : manager.getRecipes()) {
            if (r.getType() == type && resultMatches(r, data.recipeId)) list.add(r);
        }
        return list;
    }

    /** Ids de receta que desbloquean las recompensas UNLOCK_RECIPE de todas las quests. */
    private static Set<ResourceLocation> directlyLocked(RecipeManager manager, Collection<Quest> quests) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (Quest q : quests) {
            for (RewardData r : q.getRewards()) {
                if (r instanceof UnlockRecipeRewardData d) {
                    for (Recipe<?> recipe : directRecipes(manager, d)) ids.add(recipe.getId());
                }
            }
        }
        return ids;
    }

    /** Recetas que desbloquea una recompensa concreta. */
    public static List<Recipe<?>> recipesFor(RecipeManager manager, Collection<Quest> quests, RewardData reward) {
        if (reward instanceof UnlockRecipeRewardData d) return directRecipes(manager, d);
        if (reward instanceof IngredientRecipesRewardData d) {
            Set<ResourceLocation> excluded = directlyLocked(manager, quests);
            List<Recipe<?>> list = new ArrayList<>();
            for (Recipe<?> r : manager.getRecipes()) {
                if (!NPC_RECIPE_TYPES.contains(r.getType().toString())) continue;
                if (excluded.contains(r.getId())) continue;
                if (usesIngredient(r, d.ingredientId)) list.add(r);
            }
            return list;
        }
        return List.of();
    }

    /** Todas las recetas bloqueadas por alguna quest. */
    public static Set<ResourceLocation> lockedRecipes(RecipeManager manager, Collection<Quest> quests) {
        if (cachedLocked != null && cachedManager == manager && cachedQuests == quests) return cachedLocked;
        Set<ResourceLocation> ids = new HashSet<>(directlyLocked(manager, quests));
        for (Quest q : quests) {
            for (RewardData r : q.getRewards()) {
                if (r instanceof IngredientRecipesRewardData) {
                    for (Recipe<?> recipe : recipesFor(manager, quests, r)) ids.add(recipe.getId());
                }
            }
        }
        // Y las que heredan el bloqueo de su base / ingredientes
        ids.addAll(inheritedLocks(manager, quests).keySet());
        cachedManager = manager;
        cachedQuests = quests;
        cachedLocked = ids;
        return ids;
    }

    /** true si la receta solo se puede desbloquear completando una quest (lado servidor). */
    public static boolean isQuestLocked(Recipe<?> recipe) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        return lockedRecipes(server.getRecipeManager(), QuestManager.getQuests()).contains(recipe.getId());
    }
}
