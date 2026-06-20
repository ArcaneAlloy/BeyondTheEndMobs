package fr.shoqapik.btemobs.menu;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.gui.WarlockUpgradeScreen;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WarlockUpgradeMenu extends AbstractContainerMenu {
    protected final ResultContainer resultSlots = new ResultContainer();
    public final Container inputSlots = new SimpleContainer(2) {
        public void setChanged() {
            super.setChanged();
            WarlockUpgradeMenu.this.slotsChanged(this);
        }
    };
    public Optional<? extends Recipe<?>> clickedRecipe = Optional.empty();
    public ContainerData containerData;
    public final DataSlot experience;
    public final Level level;
    public final DataSlot mode;
    public final DataSlot slotEnchant;
    public WarlockRecipe recipe=null;
    public List<Enchantment> enchantments = new ArrayList<>();
    public final Player player;
    public WarlockUpgradeMenu(int id, Inventory inventory) {
        super(BteMobsContainers.WARLOCK_UPGRADE_MENU.get(), id);
        this.addSlot(new Slot(inputSlots,0,203-90,33));
        this.addSlot(new Slot(resultSlots,0,308-90,33){
            @Override
            public boolean mayPickup(Player p_40228_) {
                return true;
            }

            @Override
            public boolean mayPlace(ItemStack p_40231_) {
                return false;
            }

            @Override
            public void onTake(Player p_150645_, ItemStack p_150646_) {
                super.onTake(p_150645_, p_150646_);
                if (mode.get()==0){
                    inputSlots.getItem(0).shrink(1);
                    int countFinal = recipe.needEyes;
                    for (ItemStack stack : inventory.items){
                        if (stack.is(Items.SKELETON_SKULL)){
                            stack.shrink(Math.min(stack.getCount(),countFinal));
                            countFinal=countFinal-stack.getCount();
                        }
                        if (countFinal<=0){
                            break;
                        }
                    }
                    player.experienceLevel-=experience.get();
                } else if (mode.get()==1) {
                    inputSlots.getItem(0).shrink(1);
                    player.giveExperiencePoints(player.getXpNeededForNextLevel());
                }

            }
        });
        this.player = inventory.player;
        this.level = inventory.player.level;
        this.experience = DataSlot.standalone();
        this.mode = DataSlot.standalone();
        this.slotEnchant = DataSlot.standalone();
        this.containerData = new SimpleContainerData(1);

        this.mode.set(-1);
        this.addDataSlot(this.mode);
        this.addDataSlot(this.experience);
        this.addDataSlot(this.slotEnchant);

        this.addDataSlots(this.containerData);

        this.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int i, ItemStack itemStack) {
                if(menu != WarlockUpgradeMenu.this) return;

                if(WarlockUpgradeMenu.this.inputSlots.isEmpty()){
                    WarlockUpgradeMenu.this.clickedRecipe = Optional.empty();
                    menu.getSlot(1).set(ItemStack.EMPTY);
                    enchantments.clear();
                    recipe = null;
                    mode.set(-1);
                    return;
                }

                enchantments.clear();
                enchantments.addAll(EnchantmentHelper.getEnchantments(WarlockUpgradeMenu.this.inputSlots.getItem(0)).keySet());

                Enchantment enchantment = enchantments.get(slotEnchant.get());
                Optional<WarlockRecipe> optional = BteMobsMod.getWarlockRecipe(inventory.player)
                        .stream().filter(e -> {
                            if (enchantment == e.getEnchantment()){
                                if(EnchantmentHelper.getTagEnchantmentLevel(e.getEnchantment(),inputSlots.getItem(0))+1==e.getLevel()){
                                    return canUpgrade(inventory,e);
                                }
                            }
                            return false;
                        }).findFirst();
                optional.ifPresent((r)->recipe = r);
                if (!optional.isPresent()){
                    recipe = null;
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu menu, int i, int i1) {}
        });
        for(int k = 0; k < 3; ++k) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + k * 9 + 9, -90+184 + i1 * 18, 84 + k * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, -90+184 + l * 18, 142));
        }
    }

    public ItemStack upgrade(Recipe recipe) {
        if(recipe instanceof WarlockRecipe) {
            WarlockRecipe warlockRecipe = (WarlockRecipe) recipe;
            ItemStack base = this.inputSlots.getItem(0).copy();
            if(base.isEmpty()) return ItemStack.EMPTY;
            if (!base.isEnchanted())return ItemStack.EMPTY;
            ListTag list = base.getEnchantmentTags();
            for (int i = 0 ; i < list.size() ; i++){
                CompoundTag tag = list.getCompound(i);
                int level = EnchantmentHelper.getEnchantmentLevel(tag);
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(EnchantmentHelper.getEnchantmentId(tag));
                if(enchantment!=enchantments.get(containerData.get(0)) || warlockRecipe.getLevel()<level+1)continue;
                EnchantmentHelper.setEnchantmentLevel(tag,level+1);
            }
            return base;
        }
        return null;
    }

    public ItemStack downgrade(){
        ItemStack base = this.inputSlots.getItem(0).copy();
        if(base.isEmpty()) return ItemStack.EMPTY;
        if (!base.isEnchanted())return ItemStack.EMPTY;
        ListTag list = base.getEnchantmentTags();
        for (int i = 0 ; i < list.size() ; i++){
            CompoundTag tag = list.getCompound(i);
            int level = EnchantmentHelper.getEnchantmentLevel(tag);

            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(EnchantmentHelper.getEnchantmentId(tag));
            if(enchantment!=enchantments.get(containerData.get(0)))continue;
            if(level==1)return ItemStack.EMPTY;
            EnchantmentHelper.setEnchantmentLevel(tag,level-1);
        }
        return base;
    }

    @Override
    public boolean clickMenuButton(Player p_38875_, int p_38876_) {
        Enchantment enchantment = enchantments.get(slotEnchant.get());
        Optional<WarlockRecipe> optional = BteMobsMod.getWarlockRecipe(p_38875_)
                .stream().filter(e -> {
                    if (enchantment == e.getEnchantment()){
                        if(EnchantmentHelper.getTagEnchantmentLevel(e.getEnchantment(),inputSlots.getItem(0))+1==e.getLevel()){
                            return canUpgrade(p_38875_.getInventory(),e);
                        }
                    }
                    return false;
                }).findFirst();
        optional.ifPresent((r)->recipe = r);
        if (!optional.isPresent()){
            recipe = null;
        }
        if (recipe!=null){
            if (p_38876_ == 0){
                WarlockUpgradeMenu.this.experience.set(recipe.getExperience());
                WarlockUpgradeMenu.this.mode.set(0);

                getSlot(1).set(upgrade(recipe));
            }else if(p_38876_ > 1){
                int index = p_38876_-2;
                containerData.set(0,index);
            }
        }
        if(p_38876_ == 1){
            WarlockUpgradeMenu.this.experience.set(1);
            WarlockUpgradeMenu.this.mode.set(1);
            getSlot(1).set(downgrade());
        }

        return super.clickMenuButton(p_38875_, p_38876_);
    }

    @Override
    public void removed(Player p_38940_) {
        super.removed(p_38940_);
        this.inputSlots.clearContent();
    }

    public boolean canDowngrade(Container container){
        return true;
    }
    public boolean canUpgrade(Container container){
        int eyes = recipe == null ? 0 : recipe.needEyes;
        int exp = recipe == null ? 0 : recipe.getExperience();
        return container.hasAnyMatching((item)->item.is(Items.SKELETON_SKULL) && item.getCount() >= eyes) && player.experienceLevel>=exp;
    }
    public boolean canUpgrade(Container container,WarlockRecipe recipe){
        int eyes = recipe == null ? 0 : recipe.needEyes;
        int exp = recipe == null ? 0 : recipe.getExperience();
        return container.hasAnyMatching((item)->item.is(Items.SKELETON_SKULL) && item.getCount() >= eyes) && player.experienceLevel>=exp;
    }


    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setData(int p_38855_, int p_38856_) {

        super.setData(p_38855_, p_38856_);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
