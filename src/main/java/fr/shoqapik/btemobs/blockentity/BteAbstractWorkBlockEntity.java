package fr.shoqapik.btemobs.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the different support blocks of each npc. It is used for placing the craftingItem in the block once crafted.
 */
public abstract class BteAbstractWorkBlockEntity extends BlockEntity {

    public static Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});

    private ItemStackHandler itemHandler = new ItemStackHandler();
    private final LazyOptional<IItemHandler> inventoryOptional = LazyOptional.of(() -> this.itemHandler);

    public BteAbstractWorkBlockEntity(BlockEntityType<? extends BteAbstractWorkBlockEntity> blockEntityType, BlockPos pPos, BlockState pBlockState) {
        super(blockEntityType, pPos, pBlockState);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == ITEM_HANDLER) {
            return this.inventoryOptional.cast();
        }
        return super.getCapability(capability, direction); // See note after snippet
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.inventoryOptional.invalidate();
    }

    @Override
    public void setChanged() {

    }

}
