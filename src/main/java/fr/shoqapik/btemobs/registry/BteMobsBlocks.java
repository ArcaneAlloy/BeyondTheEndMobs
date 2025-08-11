package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.block.ExplorerTableBlock;
import fr.shoqapik.btemobs.block.MagmaForgeBlock;
import fr.shoqapik.btemobs.block.OrianaOakBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BteMobsBlocks {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BteMobsMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BteMobsMod.MODID);

    public static final RegistryObject<MagmaForgeBlock> MAGMA_FORGE = BLOCKS.register("magma_forge",
            () -> new MagmaForgeBlock(BlockBehaviour.Properties.of(Material.HEAVY_METAL, MaterialColor.METAL).noOcclusion().lightLevel((state) -> 9)));
    public static final RegistryObject<Block> EXPLORER_TABLE = registerBlock("explorer_table",
            () -> new ExplorerTableBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_PLANKS))
    );
    public static final RegistryObject<Block> ORIANA_OAK = registerBlock("oriana_oak",
            () -> new OrianaOakBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_PLANKS))
    );

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block){
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends  Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(BteMobsCreativeModeTab.BLACKSMITHE_TAB)));
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
