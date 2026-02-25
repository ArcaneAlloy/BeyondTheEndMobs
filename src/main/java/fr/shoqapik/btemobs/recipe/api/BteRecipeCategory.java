package fr.shoqapik.btemobs.recipe.api;

import fr.shoqapik.btemobs.entity.BteNpcType;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
public enum BteRecipeCategory {
    ALL(new ItemStack(Items.CRAFTING_TABLE), List.of(BteNpcType.values())),
    WEAPONS(new ItemStack(Items.NETHERITE_SWORD), List.of(BteNpcType.BLACKSMITH, BteNpcType.WARLOCK)),
    ARMORS(new ItemStack(Items.NETHERITE_CHESTPLATE), List.of(BteNpcType.BLACKSMITH, BteNpcType.WARLOCK)),
    TOOLS(new ItemStack(Items.NETHERITE_PICKAXE), List.of(BteNpcType.WARLOCK)),
    OTHERS(new ItemStack(Items.STICK), List.of(BteNpcType.BLACKSMITH));

    public final ItemStack item;
    public final List<BteNpcType> npcs;

    BteRecipeCategory(ItemStack stack, List<BteNpcType> npcs){
        this.item = stack;
        this.npcs = npcs;
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
