package fr.shoqapik.btemobs.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemRewardData extends RewardData {
    public String itemId;
    /** NBT opcional del item en formato SNBT (p. ej. {targetStructure:"dungeons_arise:shiraz_palace"}). Puede ser null. */
    public String nbt;

    public ItemRewardData(String itemId,int count) {
        this(itemId, count, null);
    }

    public ItemRewardData(String itemId, int count, String nbt) {
        super(Type.ITEM, count);
        this.itemId = itemId;
        this.nbt = (nbt == null || nbt.isEmpty()) ? null : nbt;
    }

    @Override
    public String getObjectId() {
        return this.itemId;
    }

    /** Crea el ItemStack de la recompensa, con su NBT si lo tiene. */
    public ItemStack createStack() {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id), count);
        if (nbt != null) {
            try {
                CompoundTag tag = TagParser.parseTag(nbt);
                stack.getOrCreateTag().merge(tag);
            } catch (Exception e) {
                fr.shoqapik.btemobs.BteMobsMod.LOGGER.warn("NBT no valido en la recompensa {}: {}", itemId, nbt);
            }
        }
        return stack;
    }
}
