package fr.shoqapik.btemobs.block;

import fr.shoqapik.btemobs.blockentity.BteAbstractWorkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class BteAbstractWorkBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BteAbstractWorkBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if(blockEntity instanceof BteAbstractWorkBlockEntity) {
            if(!pLevel.isClientSide) {
                // Solo el servidor extrae el item y lo da al jugador.
                // setChanged() notifica a todos los clientes via ClientboundBlockEntityDataPacket,
                // eliminando la entidad fantasma que veía el Jugador B.
                ItemStack result = blockEntity.getCapability(BteAbstractWorkBlockEntity.ITEM_HANDLER).resolve().get().extractItem(0, Integer.MAX_VALUE, false);
                if (!result.isEmpty()) {
                    Inventory inventory = pPlayer.getInventory();
                    if (inventory.player instanceof ServerPlayer) {
                        inventory.placeItemBackInInventory(result);
                    }
                    blockEntity.setChanged();
                    return InteractionResult.SUCCESS;
                }
            } else {
                // El cliente devuelve CONSUME para evitar la doble acción (swing de brazo, etc.)
                // sin manipular el inventario — espera la sincronización del servidor.
                IItemHandler handler = blockEntity.getCapability(BteAbstractWorkBlockEntity.ITEM_HANDLER).resolve().get();
                if (!handler.getStackInSlot(0).isEmpty()) {
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pPos, BlockState pState);
}
