package fr.shoqapik.btemobs.blockentity;

import fr.shoqapik.btemobs.menu.TableExplorerMenu;
import fr.shoqapik.btemobs.registry.BteMobsBlockEntities;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ExplorerTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public NonNullList<ItemStack> items=NonNullList.withSize(6, ItemStack.EMPTY);
    public ItemStack item;
    public int id;
    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            return ExplorerTableBlockEntity.this.id;
        }

        public void set(int p_58433_, int p_58434_) {
            ExplorerTableBlockEntity.this.id=p_58434_;
        }

        public int getCount() {
            return 1;
        }
    };
    public ExplorerTableBlockEntity(BlockPos p_155077_, BlockState p_155078_) {
        super(BteMobsBlockEntities.EXPLORER_TABLE_ENTITY.get(), p_155077_, p_155078_);
        this.item=ItemStack.EMPTY;
    }

    public ExplorerTableBlockEntity() {
        super(BteMobsBlockEntities.EXPLORER_TABLE_ENTITY.get(), BlockPos.ZERO, BteMobsBlocks.EXPLORER_TABLE.get().defaultBlockState());
        this.item=ItemStack.EMPTY;
    }
    public ExplorerTableBlockEntity(ItemStack stack) {
        super(BteMobsBlockEntities.EXPLORER_TABLE_ENTITY.get(), BlockPos.ZERO, BteMobsBlocks.EXPLORER_TABLE.get().defaultBlockState());
        this.item=ItemStack.EMPTY;
        this.items.set(0,stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("Table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int p_58627_, Inventory p_58628_) {
        return new TableExplorerMenu(p_58627_,p_58628_,this,this.dataAccess);
    }

    public void setData(int index,int value){
        this.dataAccess.set(index,value);
        this.setChanged();
    }

    public void setChanged() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);

            super.setChanged();
        }
    }

    public int getData(int index){
        return this.dataAccess.get(index);
    }

    public void setItem(ItemStack stack){
        this.item=stack;
        this.setChanged();
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }


    public ItemStack getItem(){
        return this.item;
    }

    @Override
    public int getContainerSize() {
        return 6;
    }

    public boolean isEmpty() {
        for(ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ItemStack getItem(int p_58328_) {
        return this.items.get(p_58328_);
    }

    public ItemStack removeItem(int p_58330_, int p_58331_) {
        return ContainerHelper.removeItem(this.items, p_58330_, p_58331_);
    }

    public ItemStack removeItemNoUpdate(int p_58387_) {
        return ContainerHelper.takeItem(this.items, p_58387_);
    }

    public void setItem(int p_58333_, ItemStack p_58334_) {
        ItemStack itemstack = this.items.get(p_58333_);
        boolean flag = !p_58334_.isEmpty() && ItemStack.isSameItemSameTags(itemstack, p_58334_);
        this.items.set(p_58333_, p_58334_);
        if (p_58334_.getCount() > this.getMaxStackSize()) {
            p_58334_.setCount(this.getMaxStackSize());
        }

        if (p_58333_ == 0 && !flag) {
            this.setChanged();
        }

    }

    @Override
    protected void saveAdditional(CompoundTag p_187461_) {
        super.saveAdditional(p_187461_);
        ContainerHelper.saveAllItems(p_187461_, this.items);
        p_187461_.putInt("id",this.id);

        if(!this.getItem().isEmpty()){
            CompoundTag tag1=new CompoundTag();
            this.getItem().save(tag1);
            p_187461_.put("item",tag1);
        }
    }

    @Override
    public void load(CompoundTag p_155080_) {
        super.load(p_155080_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_155080_, this.items);
        this.id=p_155080_.getInt("id");
        if(p_155080_.contains("item",10)){
            this.item= ItemStack.of(p_155080_.getCompound("item"));
        }
    }

    public boolean stillValid(Player p_58340_) {
        return true;
    }

    public boolean canPlaceItem(int p_58389_, ItemStack p_58390_) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction p_19238_) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_19235_, ItemStack p_19236_, @Nullable Direction p_19237_) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int p_19239_, ItemStack p_19240_, Direction p_19241_) {
        return false;
    }
}
