package fr.shoqapik.btemobs.recipe;

import com.google.gson.*;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;


public class WarlockPotionRecipe implements Recipe<SimpleContainer> {

    /**
     * Modifier slot behaviour for this recipe variant.
     *   NONE      - neither Redstone nor Glowstone accepted
     *   REDSTONE  - Redstone required (extended duration)
     *   GLOWSTONE - Glowstone dust required (amplified / level II)
     */
    public enum PotionModifier { NONE, REDSTONE, GLOWSTONE }

    /**
     * Output item type.
     *   NORMAL    - minecraft:potion
     *   SPLASH    - minecraft:splash_potion
     *   LINGERING - minecraft:lingering_potion
     */
    public enum PotionOutputType { NORMAL, SPLASH, LINGERING }

    private final BteRecipeCategory category;
    protected final ResourceLocation id;
    private final int tier;
    public final String effect;
    private final ItemStack ingredientPrimary;
    private final PotionModifier modifier;
    private final PotionOutputType outputType;

    // Legacy public fields kept for JEI compatibility (WarlockPotionCategory reads them)
    public Item type;
    public Item upgrade;

    public WarlockPotionRecipe(ItemStack stack, ResourceLocation id, String effect,
                               int tier, PotionModifier modifier, PotionOutputType outputType) {
        this.id = id;
        this.ingredientPrimary = stack;
        this.tier = tier;
        this.effect = effect;
        this.modifier = modifier;
        this.outputType = outputType;
        this.category = BteRecipeCategory.ALL;

        // Populate legacy JEI fields from enums
        this.type = switch (outputType) {
            case SPLASH    -> Items.SPLASH_POTION;
            case LINGERING -> Items.LINGERING_POTION;
            default        -> Items.POTION;
        };
        this.upgrade = switch (modifier) {
            case REDSTONE  -> Items.REDSTONE;
            case GLOWSTONE -> Items.GLOWSTONE_DUST;
            default        -> Items.AIR;
        };
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public int getTier() { return tier; }
    public PotionModifier getModifier() { return modifier; }
    public PotionOutputType getOutputType() { return outputType; }
    public ItemStack getIngredientPrimary() { return ingredientPrimary; }
    public BteRecipeCategory getCategory() { return category; }

    // ── Recipe logic ─────────────────────────────────────────────────────────

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (!container.getItem(2).is(Items.GLASS_BOTTLE)) return false;
        if (!container.getItem(3).is(ingredientPrimary.getItem())) return false;

        boolean hasRedstone   = container.getItem(0).is(Items.REDSTONE);
        boolean hasGlowstone  = container.getItem(1).is(Items.GLOWSTONE_DUST);
        boolean hasSplash     = container.getItem(4).is(Items.GUNPOWDER);
        boolean hasLingering  = container.getItem(5).is(Items.DRAGON_BREATH);

        // Modifier slots must match exactly
        if (modifier == PotionModifier.REDSTONE  && !hasRedstone)              return false;
        if (modifier == PotionModifier.GLOWSTONE && !hasGlowstone)             return false;
        if (modifier == PotionModifier.NONE      && (hasRedstone || hasGlowstone)) return false;

        // Output type slots must match exactly
        if (outputType == PotionOutputType.SPLASH    && !hasSplash)              return false;
        if (outputType == PotionOutputType.LINGERING  && !hasLingering)           return false;
        if (outputType == PotionOutputType.NORMAL     && (hasSplash || hasLingering)) return false;

        return true;
    }

    @Override
    public ItemStack assemble(SimpleContainer container) {
        return buildResult(outputType, effect);
    }

    @Override
    public ItemStack getResultItem() {
        return buildResult(outputType, effect);
    }

    /**
     * Builds the result potion directly from the recipe fields, without going
     * through PotionBrewing.mix(). This is necessary because modded potions
     * (e.g. alexsmobs:soulsteal_ii) are not registered in the vanilla brewing
     * system, so PotionBrewing.mix() would return an empty/uncraftable stack.
     * The output type (splash, lingering) and the effect ID come directly from
     * the recipe JSON fields, so no container slots are needed here.
     */
    private ItemStack buildResult(PotionOutputType type, String effectId) {
        Item potionItem = switch (type) {
            case SPLASH    -> Items.SPLASH_POTION;
            case LINGERING -> Items.LINGERING_POTION;
            default        -> Items.POTION;
        };
        // Write the Potion NBT tag directly instead of going through PotionUtils or
        // ForgeRegistries. This ensures modded potions (e.g. alexsmobs:strong_soulsteal)
        // display correctly in JEI regardless of mod load order, since we never do
        // a registry lookup that could return null.
        ItemStack stack = new ItemStack(potionItem);
        CompoundTag tag = new CompoundTag();
        tag.putString("Potion", effectId);
        stack.setTag(tag);
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return true; }

    public boolean hasItems(Player player, SimpleContainer craftContainer) {
        Inventory inventory = player.getInventory();
        return (inventory.contains(ingredientPrimary) ||
                craftContainer.hasAnyMatching(e -> e.getItem() == ingredientPrimary.getItem()))
            && (inventory.contains(new ItemStack(Items.GLASS_BOTTLE)) ||
                craftContainer.hasAnyMatching(e -> e.is(Items.GLASS_BOTTLE)));
    }

    public boolean hasItems(Inventory inventory) {
        return inventory.contains(ingredientPrimary)
            && inventory.contains(new ItemStack(Items.GLASS_BOTTLE));
    }

    @Override
    public ResourceLocation getId() { return this.id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.WARLOCK_POTION_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ingredientPrimary));
        return ingredients;
    }

    // ── Serializer ──────────────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<WarlockPotionRecipe> {

        @Override
        public WarlockPotionRecipe fromJson(ResourceLocation id, JsonObject json) {
            String effect    = GsonHelper.getAsString(json, "potion_effect");
            int tier         = GsonHelper.getAsInt(json, "tier");
            ItemStack ingredient = getItemForJson(json, "ingredient");

            // "potion_modifier": "none" | "redstone" | "glowstone"  (default: "none")
            String modStr = GsonHelper.getAsString(json, "potion_modifier", "none");
            PotionModifier modifier = switch (modStr.toLowerCase()) {
                case "redstone"  -> PotionModifier.REDSTONE;
                case "glowstone" -> PotionModifier.GLOWSTONE;
                default          -> PotionModifier.NONE;
            };

            // "potion_type": "normal" | "splash" | "lingering"  (default: "normal")
            String typeStr = GsonHelper.getAsString(json, "potion_type", "normal");
            PotionOutputType outputType = switch (typeStr.toLowerCase()) {
                case "splash"    -> PotionOutputType.SPLASH;
                case "lingering" -> PotionOutputType.LINGERING;
                default          -> PotionOutputType.NORMAL;
            };

            return new WarlockPotionRecipe(ingredient, id, effect, tier, modifier, outputType);
        }

        @Override
        public WarlockPotionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String effect    = buf.readUtf();
            int tier         = buf.readInt();
            int modOrd       = buf.readInt();
            int typeOrd      = buf.readInt();
            ItemStack ingredient = buf.readItem();
            return new WarlockPotionRecipe(ingredient, id, effect, tier,
                PotionModifier.values()[modOrd],
                PotionOutputType.values()[typeOrd]);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, WarlockPotionRecipe recipe) {
            buf.writeUtf(recipe.effect);
            buf.writeInt(recipe.tier);
            buf.writeInt(recipe.modifier.ordinal());
            buf.writeInt(recipe.outputType.ordinal());
            buf.writeItem(recipe.ingredientPrimary);
        }

        private ItemStack getItemForJson(JsonObject json, String name) {
            if (!json.has(name))
                throw new JsonSyntaxException("Missing '" + name + "', expected item or object");
            if (json.get(name).isJsonObject())
                return ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, name));
            String s = GsonHelper.getAsString(json, name);
            ResourceLocation rl = new ResourceLocation(s);
            return new ItemStack(ForgeRegistries.ITEMS.getDelegate(rl).orElseThrow(
                () -> new IllegalStateException("Item: " + s + " does not exist")));
        }
    }
}
