package fr.shoqapik.btemobs.block;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.packets.BlockUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ExplorerTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    protected static final VoxelShape AXIS_AABB = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public ExplorerTableBlock(Properties p_49795_) {
        super(p_49795_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public void onPlace(BlockState p_60566_, Level p_60567_, BlockPos p_60568_, BlockState p_60569_, boolean p_60570_) {
        if(!p_60567_.isClientSide){
            if(p_60566_.is(p_60566_.getBlock()) && p_60566_.getValue(HALF)==DoubleBlockHalf.LOWER){
                p_60566_.getBlock().setPlacedBy(p_60567_,p_60568_,p_60566_,null,ItemStack.EMPTY);
            }
        }
        super.onPlace(p_60566_, p_60567_, p_60568_, p_60569_, p_60570_);
    }

    public void openContainer(Level p_49777_, BlockPos p_49778_, Player p_49779_) {
        BlockState state=p_49777_.getBlockState(p_49778_);
        BlockEntity blockentity;
        if(state.getValue(HALF)==DoubleBlockHalf.LOWER){
            blockentity=p_49777_.getBlockEntity(p_49778_);
        }else {
            Direction direction=state.getValue(FACING);
            if(direction==Direction.EAST || direction==Direction.WEST){
                blockentity=p_49777_.getBlockEntity(p_49778_.east());
                if(!(blockentity instanceof ExplorerTableBlockEntity  && p_49777_.getBlockState(p_49778_.east()).getValue(HALF)==DoubleBlockHalf.LOWER)){
                    blockentity=p_49777_.getBlockEntity(p_49778_.west());
                }
            }else {
                blockentity=p_49777_.getBlockEntity(p_49778_.north());
                if(!(blockentity instanceof ExplorerTableBlockEntity table && p_49777_.getBlockState(p_49778_.north()).getValue(HALF)==DoubleBlockHalf.LOWER)){
                    blockentity=p_49777_.getBlockEntity(p_49778_.south());
                }
            }
        }
        if (blockentity instanceof ExplorerTableBlockEntity) {
            p_49779_.openMenu((MenuProvider)blockentity);
            //p_49779_.awardStat(Stats.INTERACT_WITH_BLAST_FURNACE);
        }
    }

    public InteractionResult use(BlockState p_48706_, Level p_48707_, BlockPos p_48708_, Player p_48709_, InteractionHand p_48710_, BlockHitResult p_48711_) {
        if (p_48707_.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            ExplorerTableBlockEntity table=this.getTableEntity(p_48707_, p_48708_, p_48709_);
            if(table!=null){
                if(p_48706_.getValue(HALF)==DoubleBlockHalf.LOWER &&
                        p_48709_.getInventory().add(table.getItem().copy())){
                    table.setItem(ItemStack.EMPTY);
                    BteMobsMod.sendToClient(new BlockUpdatePacket(p_48708_), (ServerPlayer) p_48709_);
                }
            }
            return InteractionResult.CONSUME;
        }
    }

    private boolean canOpen(Level p48707,ExplorerTableBlockEntity table) {
        ExplorerEntity explorer=getExplorer(p48707,table);
        if(explorer!=null){
            return !explorer.isCrafting();
        }else{
            return true;
        }
    }

    public ExplorerEntity getExplorer(Level level,ExplorerTableBlockEntity table){
        Entity entity=level.getEntity(table.getData(0));
        if(entity instanceof ExplorerEntity){
            return (ExplorerEntity) entity;
        }else {
            return null;
        }
    }

    private ExplorerTableBlockEntity getTableEntity(Level p_49777_, BlockPos p_49778_, Player p_49779_) {
        BlockState state=p_49777_.getBlockState(p_49778_);
        BlockEntity blockentity;
        if(state.getValue(HALF)==DoubleBlockHalf.LOWER){
            blockentity=p_49777_.getBlockEntity(p_49778_);
        }else {
            Direction direction=state.getValue(FACING);
            if(direction==Direction.EAST || direction==Direction.WEST){
                blockentity=p_49777_.getBlockEntity(p_49778_.east());
                if(!(blockentity instanceof ExplorerTableBlockEntity  && p_49777_.getBlockState(p_49778_.east()).getValue(HALF)==DoubleBlockHalf.LOWER)){
                    blockentity=p_49777_.getBlockEntity(p_49778_.west());
                }
            }else {
                blockentity=p_49777_.getBlockEntity(p_49778_.north());
                if(!(blockentity instanceof ExplorerTableBlockEntity  && p_49777_.getBlockState(p_49778_.north()).getValue(HALF)==DoubleBlockHalf.LOWER)){
                    blockentity=p_49777_.getBlockEntity(p_49778_.south());
                }
            }
        }
        if (blockentity instanceof ExplorerTableBlockEntity table) {
            return table;
        }else {
            return null;
        }
    }

    public BlockState rotate(BlockState p_48722_, Rotation p_48723_) {
        return p_48722_.setValue(FACING, p_48723_.rotate(p_48722_.getValue(FACING)));
    }

    public BlockState mirror(BlockState p_48719_, Mirror p_48720_) {
        return p_48719_.rotate(p_48720_.getRotation(p_48719_.getValue(FACING)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_60555_, BlockGetter p_60573_, BlockPos p_60574_, CollisionContext p_60575_) {
        return AXIS_AABB;
    }

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return p_49232_.getValue(HALF)==DoubleBlockHalf.UPPER ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return AXIS_AABB;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
        return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite()).setValue(HALF,DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level p_48694_, BlockPos p_48695_, BlockState p_48696_, LivingEntity p_48697_, ItemStack p_48698_) {
        if(!p_48694_.isClientSide){
            Direction direction=p_48696_.getValue(FACING);
            if(direction==Direction.EAST || direction==Direction.WEST){
                p_48694_.setBlock(p_48695_.north(),this.defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER).setValue(FACING,Direction.SOUTH),3);
                p_48694_.setBlock(p_48695_.south(),this.defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER).setValue(FACING,Direction.NORTH),3);
            }else {
                p_48694_.setBlock(p_48695_.east(),this.defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER).setValue(FACING,Direction.WEST),3);
                p_48694_.setBlock(p_48695_.west(),this.defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER).setValue(FACING,Direction.EAST),3);
            }
        }
    }

    @Override
    public void destroy(LevelAccessor p_48694_, BlockPos p_48695_, BlockState p_48696_) {
        if(!p_48694_.isClientSide()){
            if(p_48696_.getValue(HALF)==DoubleBlockHalf.LOWER){
                Direction direction=p_48696_.getValue(FACING);
                if(direction==Direction.EAST || direction==Direction.WEST){
                    p_48694_.destroyBlock(p_48695_.north(),true);
                    p_48694_.destroyBlock(p_48695_.south(),true);
                }else {
                    p_48694_.destroyBlock(p_48695_.east(),true);
                    p_48694_.destroyBlock(p_48695_.west(),true);
                }
            }else{
                Direction direction=p_48696_.getValue(FACING);
                if(direction==Direction.EAST){
                    p_48694_.destroyBlock(p_48695_.east(),true);
                    p_48694_.destroyBlock(p_48695_.east(2),true);
                }else if(direction==Direction.WEST){
                    p_48694_.destroyBlock(p_48695_.west(),true);
                    p_48694_.destroyBlock(p_48695_.west(2),true);
                }else if(direction==Direction.SOUTH){
                    p_48694_.destroyBlock(p_48695_.south(),true);
                    p_48694_.destroyBlock(p_48695_.south(2),true);
                }else {
                    p_48694_.destroyBlock(p_48695_.north(),true);
                    p_48694_.destroyBlock(p_48695_.north(2),true);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return p_153216_.getValue(HALF)==DoubleBlockHalf.LOWER ? new ExplorerTableBlockEntity(p_153215_,p_153216_) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
        super.createBlockStateDefinition(p_48725_);
        p_48725_.add(HALF).add(FACING);
    }
}
