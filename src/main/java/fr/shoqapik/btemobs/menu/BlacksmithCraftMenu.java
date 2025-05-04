package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class BlacksmithCraftMenu extends BteAbstractCraftMenu {

    public BlacksmithCraftMenu(int id, Inventory inventory, int entityId) {
        super(BteMobsContainers.BLACKSMITH_CRAFT_MENU.get(), id, inventory, entityId, 3, 3);
		// X: 62 PRIMEIRA | 80 SEGUNDA | 98 TERCEIRA
		// Y: 20 PRIMEIRA | 38 SEGUNDA
        this.addSlot(new CraftInputSlot(this.craftSlots, 0, 62, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 1, 80, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 2, 98, 20));
        this.addSlot(new CraftInputSlot(this.craftSlots, 3, 62, 38));
        this.addSlot(new CraftInputSlot(this.craftSlots, 4, 80, 38));
        this.addSlot(new CraftInputSlot(this.craftSlots, 5, 98, 38));
    }

    @Override
    public List<RecipeType<? extends BteAbstractRecipe>> getRecipeTypes() {
        return List.of(
                BteMobsRecipeTypes.BLACKSMITH_RECIPE.get(),
                BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get()
        );
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.valueOf("BLACKSMITH");
    }
}
