package fr.shoqapik.btemobs.integration.anvil_repair_jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Simple POJO representing an anvil repair entry from anvilchanges.json.
 * Not a Minecraft Recipe — it's constructed at JEI registration time
 * by reading the Anvil Changes config file.
 */
public class AnvilRepairRecipe {

    private final ResourceLocation id;
    /** The item being repaired (base). */
    private final ItemStack base;
    /** The repair material. */
    private final ItemStack repairMaterial;

    public AnvilRepairRecipe(ResourceLocation id, ItemStack base, ItemStack repairMaterial) {
        this.id = id;
        this.base = base;
        this.repairMaterial = repairMaterial;
    }

    public ResourceLocation getId() { return id; }
    public ItemStack getBase() { return base; }
    public ItemStack getRepairMaterial() { return repairMaterial; }
}
