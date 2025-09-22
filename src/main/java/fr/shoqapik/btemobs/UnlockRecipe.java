package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class UnlockRecipe {
    public final Recipe<?> recipe;
    public boolean isLock;
    public boolean wasFound;
    public UnlockRecipe(Recipe<?> recipe,boolean isLock){
        this.recipe = recipe;
        this.isLock = isLock;
        this.wasFound = false;
    }
    public UnlockRecipe(CompoundTag tag){
        this.recipe = BteMobsMod.getServer().getRecipeManager().byKey(new ResourceLocation(tag.getString("id"))).get();
        this.isLock = tag.getBoolean("isLock");
        this.wasFound = tag.getBoolean("wasFound");
    }

    public boolean is(ItemStack itemStack){
        if(this.recipe instanceof WarlockRecipe warlockRecipe && !itemStack.isEmpty()){
            if(itemStack.getItem() instanceof EnchantedBookItem){
                return EnchantmentHelper.getEnchantmentLevel(getEnchantTag(itemStack,warlockRecipe.getEnchantment()))==warlockRecipe.getLevel();
            }
            return  EnchantmentHelper.getTagEnchantmentLevel(warlockRecipe.getEnchantment(),itemStack) == warlockRecipe.getLevel();
        }
        return false;
    }
    public static CompoundTag getEnchantTag(ItemStack stack, Enchantment enchantment){
        ListTag tag = stack.getOrCreateTag().contains("StoredEnchantments") ? stack.getOrCreateTag().getList("StoredEnchantments",10) : null;
        if(tag!=null){
            ResourceLocation resourcelocation = EnchantmentHelper.getEnchantmentId(enchantment);
            for (int i = 0 ; i < tag.size() ; i++){
                CompoundTag nbt = tag.getCompound(i);
                ResourceLocation enchant = EnchantmentHelper.getEnchantmentId(nbt);
                if(resourcelocation.equals(enchant)){
                    return nbt;
                }
            }
        }
        return new CompoundTag();
    }
    public boolean is(WarlockRecipe recipe ){
        if(this.recipe instanceof WarlockRecipe warlockRecipe){
            return  recipe.getLevel() == warlockRecipe.getLevel();
        }
        return false;
    }
    public void setWasFound(boolean wasFound){
        this.wasFound = wasFound;
    }

    public void setIsLock(boolean isLock){
        this.isLock = isLock;
    }

    public CompoundTag savedData(){
        CompoundTag dataTag = new CompoundTag();
        dataTag.putBoolean("isLock",isLock);
        dataTag.putBoolean("wasFound",wasFound);
        dataTag.putString("id",recipe.getId().toString());
        return dataTag;
    }

}
