package fr.shoqapik.btemobs.integration.anvil_repair_jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.shoqapik.btemobs.BteMobsMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JEI plugin that reads anvilchanges.json from the config directory
 * and registers an "Anvil Repair" category showing:
 *   [damaged item] + [repair material] → [repaired item]
 *
 * Covers both vanilla items (whose repair materials may have been overridden
 * by AnvilChanges) and every modded item listed in the config.
 */
@JeiPlugin
public class JEIAnvilRepairPlugin implements IModPlugin {

    public static final RecipeType<AnvilRepairRecipe> ANVIL_REPAIR_TYPE =
            new RecipeType<>(AnvilRepairCategory.UID, AnvilRepairRecipe.class);

    // ── Vanilla items with their standard repair materials ────────────────────
    // These are shown in JEI even if AnvilChanges didn't override them,
    // so players have a single place to look up repair info.
    private static final String[][] VANILLA_REPAIRS = {
        // { "item_id", "repair_material_id" }
        { "minecraft:wooden_sword",    "minecraft:oak_planks" },
        { "minecraft:wooden_shovel",   "minecraft:oak_planks" },
        { "minecraft:wooden_pickaxe",  "minecraft:oak_planks" },
        { "minecraft:wooden_axe",      "minecraft:oak_planks" },
        { "minecraft:wooden_hoe",      "minecraft:oak_planks" },
        { "minecraft:stone_sword",     "minecraft:cobblestone" },
        { "minecraft:stone_shovel",    "minecraft:cobblestone" },
        { "minecraft:stone_pickaxe",   "minecraft:cobblestone" },
        { "minecraft:stone_axe",       "minecraft:cobblestone" },
        { "minecraft:stone_hoe",       "minecraft:cobblestone" },
        { "minecraft:iron_sword",      "minecraft:iron_ingot" },
        { "minecraft:iron_shovel",     "minecraft:iron_ingot" },
        { "minecraft:iron_pickaxe",    "minecraft:iron_ingot" },
        { "minecraft:iron_axe",        "minecraft:iron_ingot" },
        { "minecraft:iron_hoe",        "minecraft:iron_ingot" },
        { "minecraft:iron_helmet",     "minecraft:iron_ingot" },
        { "minecraft:iron_chestplate", "minecraft:iron_ingot" },
        { "minecraft:iron_leggings",   "minecraft:iron_ingot" },
        { "minecraft:iron_boots",      "minecraft:iron_ingot" },
        { "minecraft:golden_sword",    "minecraft:gold_ingot" },
        { "minecraft:golden_shovel",   "minecraft:gold_ingot" },
        { "minecraft:golden_pickaxe",  "minecraft:gold_ingot" },
        { "minecraft:golden_axe",      "minecraft:gold_ingot" },
        { "minecraft:golden_hoe",      "minecraft:gold_ingot" },
        { "minecraft:golden_helmet",   "minecraft:gold_ingot" },
        { "minecraft:golden_chestplate","minecraft:gold_ingot" },
        { "minecraft:golden_leggings", "minecraft:gold_ingot" },
        { "minecraft:golden_boots",    "minecraft:gold_ingot" },
        { "minecraft:diamond_sword",   "minecraft:diamond" },
        { "minecraft:diamond_shovel",  "minecraft:diamond" },
        { "minecraft:diamond_pickaxe", "minecraft:diamond" },
        { "minecraft:diamond_axe",     "minecraft:diamond" },
        { "minecraft:diamond_hoe",     "minecraft:diamond" },
        { "minecraft:diamond_helmet",  "minecraft:diamond" },
        { "minecraft:diamond_chestplate","minecraft:diamond" },
        { "minecraft:diamond_leggings","minecraft:diamond" },
        { "minecraft:diamond_boots",   "minecraft:diamond" },
        { "minecraft:netherite_sword",    "minecraft:netherite_ingot" },
        { "minecraft:netherite_shovel",   "minecraft:netherite_ingot" },
        { "minecraft:netherite_pickaxe",  "minecraft:netherite_ingot" },
        { "minecraft:netherite_axe",      "minecraft:netherite_ingot" },
        { "minecraft:netherite_hoe",      "minecraft:netherite_ingot" },
        { "minecraft:netherite_helmet",   "minecraft:netherite_ingot" },
        { "minecraft:netherite_chestplate","minecraft:netherite_ingot" },
        { "minecraft:netherite_leggings", "minecraft:netherite_ingot" },
        { "minecraft:netherite_boots",    "minecraft:netherite_ingot" },
        { "minecraft:leather_helmet",     "minecraft:leather" },
        { "minecraft:leather_chestplate", "minecraft:leather" },
        { "minecraft:leather_leggings",   "minecraft:leather" },
        { "minecraft:leather_boots",      "minecraft:leather" },
        { "minecraft:chainmail_helmet",   "minecraft:iron_ingot" },
        { "minecraft:chainmail_chestplate","minecraft:iron_ingot" },
        { "minecraft:chainmail_leggings", "minecraft:iron_ingot" },
        { "minecraft:chainmail_boots",    "minecraft:iron_ingot" },
        { "minecraft:turtle_helmet",      "minecraft:scute" },
        { "minecraft:bow",                "minecraft:string" },
        { "minecraft:crossbow",           "minecraft:string" },
        { "minecraft:trident",            "minecraft:prismarine_shard" },
        { "minecraft:elytra",             "minecraft:phantom_membrane" },
        { "minecraft:fishing_rod",        "minecraft:string" },
        { "minecraft:flint_and_steel",    "minecraft:flint" },
        { "minecraft:shears",             "minecraft:iron_ingot" },
        { "minecraft:shield",             "minecraft:iron_ingot" },
    };

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BteMobsMod.MODID, "jei_anvil_repair_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
            new AnvilRepairCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<AnvilRepairRecipe> recipes = new ArrayList<>();

        // ── 1. Vanilla base repairs ───────────────────────────────────────────
        for (String[] pair : VANILLA_REPAIRS) {
            AnvilRepairRecipe r = buildRecipe(pair[0], pair[1], "vanilla");
            if (r != null) recipes.add(r);
        }

        // ── 2. AnvilChanges overrides (config/anvilchanges.json) ──────────────
        // AnvilChanges overrides the repair material; we show the NEW one.
        // Items already listed in VANILLA_REPAIRS will appear twice in JEI
        // (once with the vanilla material, once with the modded material)
        // which is intentional — players can see what changed.
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("anvilchanges.json");
        if (configPath.toFile().exists()) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray changes = root.getAsJsonArray("objectChanges");
                // Deduplicate: skip if we already added an identical base+repair pair
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JsonElement el : changes) {
                    JsonObject obj = el.getAsJsonObject();
                    String base   = obj.get("base").getAsString();
                    String repair = obj.get("newrepair").getAsString();
                    String key    = base + "|" + repair;
                    if (seen.add(key)) {
                        AnvilRepairRecipe r = buildRecipe(base, repair, "anvilchanges");
                        if (r != null) recipes.add(r);
                    }
                }
                System.out.println("[BteMobs] AnvilRepair JEI: loaded "
                    + changes.size() + " entries from anvilchanges.json");
            } catch (Exception e) {
                System.err.println("[BteMobs] AnvilRepair JEI: failed to read anvilchanges.json — " + e.getMessage());
            }
        } else {
            System.out.println("[BteMobs] AnvilRepair JEI: anvilchanges.json not found at " + configPath);
        }

        if (!recipes.isEmpty()) {
            registration.addRecipes(ANVIL_REPAIR_TYPE, recipes);
            System.out.println("[BteMobs] AnvilRepair JEI: registered " + recipes.size() + " repair entries.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds an AnvilRepairRecipe from two resource-location strings.
     * Returns null (and logs a warning) if either item is not found in the registry.
     */
    private static AnvilRepairRecipe buildRecipe(String baseId, String materialId, String source) {
        Item baseItem   = resolveItem(baseId);
        Item matItem    = resolveItem(materialId);
        if (baseItem == null) {
            System.out.println("[BteMobs] AnvilRepair JEI: unknown item '" + baseId
                + "' (source: " + source + ") — skipping");
            return null;
        }
        if (matItem == null) {
            System.out.println("[BteMobs] AnvilRepair JEI: unknown repair material '"
                + materialId + "' for '" + baseId
                + "' (source: " + source + ") — skipping");
            return null;
        }
        ResourceLocation id = new ResourceLocation(
            BteMobsMod.MODID,
            "anvil_repair/" + source + "/"
                + baseId.replace(':', '_').replace('/', '_')
        );
        return new AnvilRepairRecipe(id, new ItemStack(baseItem), new ItemStack(matItem));
    }

    private static Item resolveItem(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            // ForgeRegistries returns air for unknown items — treat that as missing
            if (item == null || item == net.minecraft.world.item.Items.AIR) return null;
            return item;
        } catch (Exception e) {
            return null;
        }
    }
}
