package fr.shoqapik.btemobs.menu;

import net.minecraftforge.fml.ModList;
import fr.shoqapik.btemobs.registry.BteMobsContainers;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import com.patataprojects.anvilchanges.repairs.RepairConfig;

import java.util.List;
import java.util.Map;


public class BlacksmithRepairMenu extends ItemCombinerMenu {

    public int repairItemCountCost;
    private String itemName;

    public BlacksmithRepairMenu(int pContainerId, Inventory pPlayerInventory) {
        super(BteMobsContainers.BLACKSMITH_REPAIR_MENU.get(), pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    @Override
    protected boolean mayPickup(Player pPlayer, boolean pHasStack) {
        return true;
    }

    @Override
    protected void onTake(Player p_150474_, ItemStack p_150475_) {
        this.inputSlots.setItem(0, ItemStack.EMPTY);
        if (this.repairItemCountCost > 0) {
            ItemStack itemstack = this.inputSlots.getItem(1);
            if (!itemstack.isEmpty() && itemstack.getCount() > this.repairItemCountCost) {
                itemstack.shrink(this.repairItemCountCost);
                this.inputSlots.setItem(1, itemstack);
            } else {
                this.inputSlots.setItem(1, ItemStack.EMPTY);
            }
        } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
        }
    }

    @Override
    protected boolean isValidBlock(BlockState pState) {
        return false;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.clearContainer(pPlayer, this.inputSlots);
    }

    @Override
    public void createResult() {
        ItemStack itemstack = this.inputSlots.getItem(0);

        //-------------------------------------------------------------------------------------------------------
        Map<String, List<String>> repairRules = RepairConfig.getAllRepairRules();

            if (itemstack.isEmpty()) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            } else {
                ItemStack itemstack1 = itemstack.copy();
                ItemStack itemstack2 = this.inputSlots.getItem(1);

                if (!itemstack2.isEmpty()) {
                    if (itemstack.isDamageableItem() && repairRules.containsKey(itemstack1.getDescriptionId()) && repairRules.get(itemstack1.getDescriptionId()).get(0).equals(ForgeRegistries.ITEMS.getKey(itemstack2.getItem()).toString())){

                        double percentage = 0.33;
                        if (isNugget(itemstack2)) percentage = 0.03;

                        int i = 0;
                        int damage = itemstack1.getDamageValue();
                        for (i = 0; i < itemstack2.getCount(); ++i) {
                            damage = (int) (damage - itemstack1.getMaxDamage() * percentage);
                            //l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                        }
                        itemstack1.setDamageValue(damage);

                        this.repairItemCountCost = i;

                    }
                    else if (itemstack1.isDamageableItem() && isValidRepairItem(itemstack, itemstack2) && !repairRules.containsKey(itemstack1.getDescriptionId())) {

                    /*int l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                    if (l2 <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        return;
                    }*/

                        double percentage = 0.33;
                        if (isNugget(itemstack2)) percentage = 0.03;

                        int i = 0;
                        int damage = itemstack1.getDamageValue();
                        for (i = 0; i < itemstack2.getCount(); ++i) {
                            damage = (int) (damage - itemstack1.getMaxDamage() * percentage);
                            //l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                        }
                        itemstack1.setDamageValue(damage);

                        this.repairItemCountCost = i;
                    } else if (itemstack1.isDamageableItem() && itemstack1.is(itemstack2.getItem())) {
                        int durability1 = itemstack1.getMaxDamage() - itemstack1.getDamageValue();
                        int durability2 = itemstack2.getMaxDamage() - itemstack2.getDamageValue();

                        int damage = itemstack1.getMaxDamage() - (durability1 + durability2);
                        if (damage < 0) {
                            damage = 0;
                        }
                        itemstack1.setDamageValue(damage);
                        this.repairItemCountCost = 1;
                    } else {
                        itemstack1 = ItemStack.EMPTY;
                    }
                }

                if (StringUtils.isBlank(this.itemName)) {
                    if (itemstack.hasCustomHoverName()) {
                        itemstack1.resetHoverName();
                    }
                } else if (!this.itemName.equals(itemstack.getHoverName().getString())) {
                    itemstack1.setHoverName(Component.literal(this.itemName));
                }

                this.resultSlots.setItem(0, itemstack1);
                this.broadcastChanges();
            }
    }

    public void setItemName(String pNewName) {
        this.itemName = pNewName;
        if (this.getSlot(2).hasItem()) {
            ItemStack itemstack = this.getSlot(2).getItem();
            if (StringUtils.isBlank(pNewName)) {
                itemstack.resetHoverName();
            } else {
                itemstack.setHoverName(Component.literal(this.itemName));
            }
        }

        this.createResult();
    }

    public boolean isValidRepairItem(ItemStack itemstack, ItemStack repairCandidate) {
        if(itemstack.getItem().isValidRepairItem(itemstack, repairCandidate)) {
            return true;
        } else {
            ResourceLocation itemLocation = ForgeRegistries.ITEMS.getKey(repairCandidate.getItem());
            if(itemLocation.getPath().endsWith("_nugget")) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemLocation.getNamespace(), itemLocation.getPath().replace("_nugget", "_ingot")));
                if(item != null) {
                    if(itemstack.getItem().isValidRepairItem(itemstack, new ItemStack(item))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isNugget(ItemStack itemStack) {
        ResourceLocation itemLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(itemLocation.getPath().endsWith("_nugget")) {
            return true;
        }

        return false;
    }
}