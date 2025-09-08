package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.List;
import java.util.Optional;

public class WarlockCraftMenu extends BteAbstractCraftMenu {

    protected BteAbstractCraftContainer baseSlots;
    protected ResultContainer resultSlots;
    public final DataSlot experience;

    public Optional<? extends Recipe<?>> clickedRecipe = Optional.empty();

    public WarlockCraftMenu(int id, Inventory inventory, int entityId) {
        super(BteMobsContainers.WARLOCK_CRAFT_MENU.get(), id, inventory, entityId, 4, 1, 5);

        this.experience = DataSlot.standalone();
        this.addDataSlot(this.experience);

        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int i, ItemStack itemStack) {
                if(player.level.isClientSide) return;
                if(menu != WarlockCraftMenu.this) return;
                if(menu.getSlot(i).container != WarlockCraftMenu.this.craftSlots && menu.getSlot(i).container != WarlockCraftMenu.this.baseSlots) return;
                List<Recipe> recipes = getCraftableRecipes((ServerPlayer)player);
                if(recipes.isEmpty()) {
                    WarlockCraftMenu.this.resultSlots.setItem(0, ItemStack.EMPTY);
                    WarlockCraftMenu.this.experience.set(0);
                    WarlockCraftMenu.this.experience.set(0);
                    return;
                }
                Recipe<?> recipe = recipes.get(0);
                if (clickedRecipe.isPresent() && recipes.contains(clickedRecipe.get())) {
                    recipe = clickedRecipe.get();
                }
                ItemStack result = assembleResult(recipe);
                WarlockCraftMenu.this.resultSlots.setItem(0, result);
                if(recipes.get(0) instanceof WarlockRecipe && !result.isEmpty()) {
                    WarlockCraftMenu.this.experience.set(((WarlockRecipe)recipes.get(0)).getExperience());
                } else {
                    WarlockCraftMenu.this.experience.set(0);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int i1) {}
        });
    }

    @Override
    public void initCraftingSlot() {
        baseSlots = new BteAbstractCraftContainer(this, 1, 1, 1);
        resultSlots = new ResultContainer();

        // X: 11 PRIMEIRA | 29 SEGUNDA | 47 TERCEIRA | 90 QUARTA | 148 QUINTA (RESULT)
        // Y: 25 PRIMEIRA
        this.addSlot(new CraftInputSlot(this.craftSlots, 0, 11, 25));
        this.addSlot(new CraftInputSlot(this.craftSlots, 1, 29, 25));
        this.addSlot(new CraftInputSlot(this.craftSlots, 2, 47, 25));

        this.addSlot(new CraftInputSlot(this.baseSlots, 0, 90, 25));

        this.addSlot(new Slot(this.resultSlots, 0, 148, 25) {
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            public boolean mayPickup(Player player) {
                if(player.level.isClientSide) return false;
                List<Recipe> recipes = getCraftableRecipes((ServerPlayer)player);
                if(recipes.isEmpty()) return false;
                return hasRequirementsForCraft(recipes.get(0));
            }

            public void onTake(Player player, ItemStack itemStack) {
                if(player.level.isClientSide) return;
                WarlockCraftMenu.this.baseSlots.getItem(0).shrink(1);
                craftItemServer((ServerPlayer) player, WarlockCraftMenu.this.clickedRecipe);
                WarlockCraftMenu.this.experience.set(0);
                WarlockCraftMenu.this.clickedRecipe = Optional.empty();
            }
        });
    }

    @Override
    public List<RecipeType<? extends BteAbstractRecipe>> getRecipeTypes() {
        return List.of(BteMobsRecipeTypes.WARLOCK_RECIPE.get());
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return BteMobsMod.WARLOCK;
    }

    @Override
    public boolean hasRequirementsForCraft(Recipe<?> recipe) {
        if(recipe instanceof WarlockRecipe) {
            return player.experienceLevel >= ((WarlockRecipe) recipe).getExperience();
        } else {
            return true;
        }
    }

    @Override
    public ItemStack assembleResult(Recipe recipe) {
        if(recipe instanceof WarlockRecipe) {
            WarlockRecipe warlockRecipe = (WarlockRecipe) recipe;
            ItemStack base = this.baseSlots.getItem(0).copy();
            if(base.isEmpty() || !warlockRecipe.getEnchantment().canEnchant(base)) return ItemStack.EMPTY;
            base.setCount(1);
            base.enchant(warlockRecipe.getEnchantment(), warlockRecipe.getLevel());
            return base;
        }
        return super.assembleResult(recipe);
    }

    @Override
    public void placeResult(Recipe<?> recipe, ItemStack result) {
        if(recipe instanceof WarlockRecipe) {
            WarlockRecipe warlockRecipe = (WarlockRecipe) recipe;
            player.giveExperienceLevels(-warlockRecipe.getExperience());
        }
    }
}
