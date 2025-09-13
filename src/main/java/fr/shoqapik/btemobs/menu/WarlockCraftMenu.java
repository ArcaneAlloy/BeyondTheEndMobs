package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import fr.shoqapik.btemobs.menu.slot.CraftInputSlot;
import fr.shoqapik.btemobs.packets.PlaceGhostRecipePacket;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarlockCraftMenu extends AbstractContainerMenu {
    public SimpleContainer craftSlots;

    protected BteAbstractCraftContainer baseSlots;
    public ResultContainer resultSlots;
    public final DataSlot experience;
    public final Level level;
    public Optional<? extends Recipe<?>> clickedRecipe = Optional.empty();
    public final int entityId;

    public WarlockCraftMenu(int id, Inventory inventory, int entityId) {
        super(BteMobsContainers.WARLOCK_CRAFT_MENU.get(),id);
        this.initCraftingSlot();
        this.experience = DataSlot.standalone();
        this.addDataSlot(this.experience);
        this.entityId = entityId;
        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int i, ItemStack itemStack) {
                if(menu != WarlockCraftMenu.this) return;
                if(menu.getSlot(i).container != WarlockCraftMenu.this.craftSlots
                        && menu.getSlot(i).container != WarlockCraftMenu.this.baseSlots) return;
                Optional<WarlockRecipe> optional = WarlockCraftMenu.this.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get()).parallelStream().filter(e->e.matches(WarlockCraftMenu.this.craftSlots,WarlockCraftMenu.this.level)).findAny();
                if(optional.isPresent() && !baseSlots.getItem(0).isEmpty() && (optional.get().getEnchantment().canEnchant(baseSlots.getItem(0)) || baseSlots.getItem(0).is(Items.BOOK)) || baseSlots.getItem(0).is(Items.ENCHANTED_BOOK)){
                    WarlockCraftMenu.this.clickedRecipe = optional;
                    WarlockCraftMenu.this.experience.set(optional.get().getExperience());
                    menu.getSlot(4).set(optional.get().assemble(WarlockCraftMenu.this.getTotalContainer()));
                }else {
                    WarlockCraftMenu.this.clickedRecipe = null;
                    menu.getSlot(4).set(ItemStack.EMPTY);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int i1) {}
        });

        this.level = inventory.player.level;
        for(int k = 0; k < 3; ++k) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, 161 + i1 * 18, 84 + k * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 161 + l * 18, 142));
        }
    }
    public void initCraftingSlot() {
        craftSlots = new SimpleContainer(3);
        baseSlots = new BteAbstractCraftContainer(this, 1, 1, 1);
        resultSlots = new ResultContainer();
        // X: 11 PRIMEIRA | 29 SEGUNDA | 47 TERCEIRA | 90 QUARTA | 148 QUINTA (RESULT)
        // Y: 25 PRIMEIRA
        this.addSlot(new CraftInputSlot(this.craftSlots, 0, 164, 25));
        this.addSlot(new CraftInputSlot(this.craftSlots, 1, 182, 25));
        this.addSlot(new CraftInputSlot(this.craftSlots, 2, 200, 25));

        this.addSlot(new CraftInputSlot(this.baseSlots, 0, 243, 25));

        this.addSlot(new Slot(this.resultSlots, 0, 301, 25) {
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            public boolean mayPickup(Player player) {
                return (player.getAbilities().instabuild || player.experienceLevel >= WarlockCraftMenu.this.experience.get()) && WarlockCraftMenu.this.experience.get() > 0;
            }

            public void onTake(Player player, ItemStack itemStack) {
                if(WarlockCraftMenu.this.clickedRecipe.isPresent() && WarlockCraftMenu.this.clickedRecipe.get() instanceof WarlockRecipe recipe){
                    WarlockCraftMenu.this.baseSlots.getItem(0).shrink(1);
                    for (int i = 0 ; i < WarlockCraftMenu.this.craftSlots.getContainerSize() ; i++){
                        ItemStack stack = WarlockCraftMenu.this.craftSlots.getItem(i);
                        ItemStack required = getRequiredItem(recipe.requiredItem,stack);
                        if(!stack.isEmpty() && !required.isEmpty()){
                            stack.shrink(required.getCount());
                        }
                    }
                    player.experienceLevel-=WarlockCraftMenu.this.experience.get();
                    WarlockCraftMenu.this.experience.set(0);
                    WarlockCraftMenu.this.clickedRecipe = Optional.empty();
                }

            }

            public ItemStack getRequiredItem(Ingredient ingredient , ItemStack stack){
                for (int i = 0 ; i<ingredient.getItems().length ; i++){
                    ItemStack stack1 = ingredient.getItems()[i];
                    if(stack1.getItem() == stack.getItem()){
                        return stack1;
                    }
                }
                return ItemStack.EMPTY;
            }
        });
    }
    public SimpleContainer getTotalContainer (){
        return new SimpleContainer(this.craftSlots.getItem(0), this.craftSlots.getItem(1), this.craftSlots.getItem(2), this.baseSlots.getItem(0));
    }

    public int getEntityId(){
        return this.entityId;
    }

    public ItemStack assemble(Recipe recipe) {
        if(recipe instanceof WarlockRecipe) {
            WarlockRecipe warlockRecipe = (WarlockRecipe) recipe;
            ItemStack base = this.baseSlots.getItem(0).copy();
            if(base.isEmpty() || !warlockRecipe.getEnchantment().canEnchant(base)) return ItemStack.EMPTY;
            base.setCount(1);
            base.enchant(warlockRecipe.getEnchantment(), warlockRecipe.getLevel());
            return base;
        }
        return null;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        clearContainer(pPlayer,craftSlots);
        clearContainer(pPlayer,baseSlots);
    }


    public boolean recipeMatches(WarlockRecipe recipe){
        return recipe.matches(craftSlots,level);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    public void placeRecipe(ServerPlayer player, ItemStack item) {
        Optional<WarlockRecipe> optionalRecipe = player.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE .get()).stream().filter(e->{
            return EnchantmentHelper.getTagEnchantmentLevel(e.getEnchantment(),item) == e.getLevel();
        }).findFirst();
        for(int i = 0; i < 3; i++){
            ItemStack slot = craftSlots.getItem(i);
            if(!slot.isEmpty()){
                player.getInventory().add(slot);
                craftSlots.setItem(i,ItemStack.EMPTY);
            }
        }

        if(optionalRecipe.isPresent()){
            WarlockRecipe recipe=optionalRecipe.get();
            if(recipe.hasItems(player)){
                int index = 0;
                for(ItemStack requieredItem: recipe.getRequiredItems().getItems()){
                    int removed = 0;
                    ItemStack placeResult = null;
                    for(ItemStack stack : player.getInventory().items){
                        if(stack.getItem() == requieredItem.getItem()){
                            BteMobsMod.LOGGER.debug("Count :"+requieredItem.getItem()+" "+requieredItem.getCount());
                            if(removed < requieredItem.getCount()) {
                                boolean finish = false;
                                int toRemove = requieredItem.getCount() - removed;
                                if (toRemove > stack.getCount()){
                                    toRemove = stack.getCount();
                                }else {
                                    finish = true;
                                    placeResult = stack.copy();
                                    placeResult.setCount(requieredItem.getCount());
                                }
                                stack.shrink(toRemove);
                                removed += toRemove;
                                if(finish){
                                    break;
                                }
                            }else {
                                placeResult = stack.copy();
                                placeResult.setCount(requieredItem.getCount());
                                break;
                            }

                        }
                    }
                    this.craftSlots.setItem(index,placeResult!=null ? placeResult : ItemStack.EMPTY);
                    this.slotsChanged(this.craftSlots);
                    index ++;
                }
            }else {
                BteMobsMod.sendToClient(new PlaceGhostRecipePacket(this.containerId,recipe),player);
            }
        }
    }
}
