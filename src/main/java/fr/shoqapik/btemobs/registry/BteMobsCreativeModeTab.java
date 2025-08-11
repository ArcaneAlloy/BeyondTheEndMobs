package fr.shoqapik.btemobs.registry;

import com.google.common.collect.Ordering;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

public class BteMobsCreativeModeTab {
    static Comparator<ItemStack> stackComparator;
    public static final CreativeModeTab BLACKSMITHE_TAB = new CreativeModeTab("blacksmith_tab") {

        @Override
        public ItemStack makeIcon() {
            return new ItemStack(BteMobsBlocks.EXPLORER_TABLE.get());
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> pItems) {
            super.fillItemList(pItems);
            preOrdenInit();
            pItems.sort(stackComparator);
        }
    };

    public static void preOrdenInit(){
        List<Item> itemList= List.of(
                BteMobsBlocks.EXPLORER_TABLE.get().asItem(),
                BteMobsBlocks.ORIANA_OAK.get().asItem()
        );

        stackComparator= Ordering.explicit(itemList).onResultOf(ItemStack::getItem);
    }


}
