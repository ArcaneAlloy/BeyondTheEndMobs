package fr.shoqapik.btemobs.recipe.api;

import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
public enum BteRecipeCategory {
    ALL(new ItemStack(Items.CRAFTING_TABLE), List.of(BteNpcType.values())),
    WEAPONS(new ItemStack(Items.NETHERITE_SWORD), List.of(BteNpcType.BLACKSMITH, BteNpcType.WARLOCK)),
    ARMORS(new ItemStack(Items.NETHERITE_CHESTPLATE), List.of(BteNpcType.BLACKSMITH, BteNpcType.WARLOCK)),
    TOOLS(new ItemStack(Items.NETHERITE_PICKAXE), List.of(BteNpcType.BLACKSMITH, BteNpcType.WARLOCK)),
    ARTIFACTS(new ItemStack(Items.TOTEM_OF_UNDYING), "dungeons_gear:iron_hide_amulet", List.of(BteNpcType.BLACKSMITH)),
    OTHERS(new ItemStack(Items.STICK), "endrem:exotic_eye", List.of(BteNpcType.BLACKSMITH));

    /** Icono de reserva (el que se registra en el recipe book al arrancar). */
    public final ItemStack item;
    public final List<BteNpcType> npcs;
    /** Icono de otro mod; se resuelve tarde porque al registrar las categorias puede no existir aun. */
    @Nullable
    private final ResourceLocation iconId;
    @Nullable
    private ItemStack resolvedIcon;

    BteRecipeCategory(ItemStack stack, List<BteNpcType> npcs){
        this(stack, null, npcs);
    }

    BteRecipeCategory(ItemStack stack, @Nullable String iconId, List<BteNpcType> npcs){
        this.item = stack;
        this.iconId = iconId == null ? null : new ResourceLocation(iconId);
        this.npcs = npcs;
    }

    public boolean hasCustomIcon() {
        return this.iconId != null;
    }

    /** Icono a mostrar: el item de iconId si existe, si no el de reserva. */
    public ItemStack getIcon() {
        if (this.iconId == null) return this.item;
        if (this.resolvedIcon == null) {
            Item icon = ForgeRegistries.ITEMS.getValue(this.iconId);
            if (icon == null || icon == Items.AIR) return this.item;
            this.resolvedIcon = new ItemStack(icon);
        }
        return this.resolvedIcon;
    }

    public static List<BteRecipeCategory> values(BteNpcType npcType) {
        List<BteRecipeCategory> categories = new ArrayList<>();
        for (BteRecipeCategory category : BteRecipeCategory.values()) {
            if (category.npcs.contains(npcType)) {
                categories.add(category);
            }
        }
        return categories;
    }
}
