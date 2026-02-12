package fr.shoqapik.btemobs.entity;

import com.mojang.math.Vector3f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.block.OrianaOakBlock;
import fr.shoqapik.btemobs.menu.DruidMenu;
import fr.shoqapik.btemobs.packets.DirectionPacket;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class DruidEntity extends BteAbstractEntity implements WorldlyContainer,ContainerListener,MenuProvider {
    public int craftingTime=0;
    public int animIdle=0;
    public ItemStack resultItem=ItemStack.EMPTY;
    public IdleAnim anim = IdleAnim.SIT_1;
    public ItemPart craftingItem;
    public ItemPart sample1;
    public ItemPart sample2;
    public ItemPart sample3;
    public ItemPart[] samplesItems;
    public ItemPart[] items;
    public BlockPos tablePos = null;
    static final Vector3f FORWARD = Util.make(() -> {
        Vec3i vec3i = Direction.SOUTH.getNormal();
        return new Vector3f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
    });

    public SimpleContainer inventory;
    public int id;
    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            return DruidEntity.this.getId();
        }

        public void set(int p_58433_, int p_58434_) {

        }

        public int getCount() {
            return 1;
        }
    };
    protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(DruidEntity.class, EntityDataSerializers.DIRECTION);
    protected static final EntityDataAccessor<Boolean> CRAFTING = SynchedEntityData.defineId(DruidEntity.class, EntityDataSerializers.BOOLEAN);

    public DruidEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.lookControl = new NpcLookControl(this);
        this.craftingItem = new ItemPart("result" , this,0.25F,0.25F);
        this.sample1 = new ItemPart("sample1",this,0.25F,0.25F,true);
        this.sample2 = new ItemPart("sample2",this,0.25F,0.25F,true);
        this.sample3 = new ItemPart("sample3",this,0.25F,0.25F,true);

        this.samplesItems = new ItemPart[]{
                this.sample1,this.sample2,this.sample3
        };
        this.items = new ItemPart[]{
                this.craftingItem,this.sample1,this.sample2,this.sample3
        };
        this.setId(ENTITY_COUNTER.getAndAdd(this.items.length+1) + 1);
        this.createInventory();
    }
    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(6);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.inventory));
    }
    @Override
    public void setId(int p_20235_) {
        super.setId(p_20235_);
        for (int i = 0; i < this.items.length; i++) // Forge: Fix MC-158205: Set part ids to successors of parent mob id
            this.craftingItem.setId(p_20235_ + i + 1);
    }
    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public BteNpcType getNpcType() {
        return BteNpcType.DRUID;
    }

    @Override
    public void tick() {
        this.setDeltaMovement(new Vec3(0,this.getDeltaMovement().y,0));

        super.tick();

        if(this.items!=null){
            for(ItemPart part : this.items){
                part.tick();
            }
            float yawRad = (float)Math.toRadians(this.getYRot());
            float sin = (float)Math.sin(yawRad);
            float cos = (float)Math.cos(yawRad);

            double body0X =  (0 * cos) + -0.8F *sin;
            double body0Z = (0 * sin )-(-0.8F * cos);


            Vec3 delta = this.craftingItem.getDeltaMovement();

            Vec3 pos = new Vec3(this.craftingItem.getX(), this.craftingItem.getY(), this.craftingItem.getZ());

            Vec3[] vec3s = new Vec3[this.samplesItems.length];
            for(int l=0;l<this.samplesItems.length;l++){
                vec3s[l] = new Vec3(this.samplesItems[l].getX(),this.samplesItems[l].getY(),this.samplesItems[l].getZ());
            }
            if(this.craftingItem.craftingFinalize){
                this.tickPartDelta(this.craftingItem,delta.x,delta.y,delta.z);
            }else {
                this.tickPart(this.craftingItem,body0X,1.7F,body0Z);
            }


            this.tickPart(this.sample1,cos*1.5F + sin*0.7F,2.1F+Mth.cos(this.tickCount*0.05F)*0.25F,sin*1.5F -cos*0.7F);
            this.tickPart(this.sample2,-(cos*1.5F + sin*0.7F),2.1F+Mth.cos(this.tickCount*0.05F - 15.0F)*0.25F,-(sin*1.5F -cos*0.7F));
            this.tickPart(this.sample3,0,3.1F+Mth.cos(this.tickCount*0.05F - 30.0F)*0.25F,-body0Z);


            this.craftingItem.xo = pos.x;
            this.craftingItem.yo = pos.y;
            this.craftingItem.zo = pos.z;
            this.craftingItem.xOld = pos.x;
            this.craftingItem.yOld = pos.y;
            this.craftingItem.zOld = pos.z;
            for (int k = 0 ; k<this.samplesItems.length ; k++){
                this.samplesItems[k].xo = vec3s[k].x;
                this.samplesItems[k].yo = vec3s[k].y;
                this.samplesItems[k].zo = vec3s[k].z;
                this.samplesItems[k].xOld = vec3s[k].x;
                this.samplesItems[k].yOld = vec3s[k].y;
                this.samplesItems[k].zOld = vec3s[k].z;
            }
        }

        if(this.animIdle>=0){
            this.animIdle--;
            if(this.animIdle<=0){
                if(!this.level.isClientSide){
                    int nextIdleAnim = this.random.nextInt(0,3);
                    this.startNextIdleAnim(nextIdleAnim);
                }
            }
        }
        if(this.isCrafting()){
            this.craftingTime--;
            if(this.craftingTime==175){
                this.startCreationCrafting();
            }else if(this.craftingTime == 110){
                float yawRad = (float)Math.toRadians(this.getYRot());
                float sin = (float)Math.sin(yawRad);
                float cos = (float)Math.cos(yawRad);

                double body0X =  (-0.6 * cos) + (-2.1F) * sin;
                double body0Z = ( -0.6 * sin )-((-2.1F) * cos);

                double x = body0X+this.getX();
                double y = this.getY()+1.2F;
                double z = body0Z+this.getZ();

                Vec3 delta = new Vec3((x)-this.craftingItem.getX(),(y)-this.craftingItem.getY(),(z)-this.craftingItem.getZ());
                if (delta.length()>0.0001F){
                    delta = delta.normalize().scale(0.035F);
                    this.craftingItem.setDeltaMovement(delta.x,delta.y,delta.z);
                }else {
                    this.setPos(x,y,z);
                    this.craftingItem.setDeltaMovement(Vec3.ZERO);
                }
            }else if(this.craftingTime==90){
                this.craftingItem.setDeltaMovement(0,0,0);
            }else if(this.craftingTime==0){
                this.setCrafting(false);
            }
        }

        if(this.tablePos!=null){
            this.setPos(this.tablePos.getX()+0.9F,this.tablePos.getY()+2.7755575615628914E-17,this.tablePos.getZ()+0.9F);
        }

        this.setYBodyRot(this.getAttachFace().toYRot());
        this.setYRot(this.getAttachFace().toYRot());
        this.setRot(this.getAttachFace().toYRot(),this.getXRot());
    }

    public void startCreationCrafting(){
        this.craftingItem.startCreationItem(this.resultItem);
    }

    private void startNextIdleAnim(int nextIdleAnim) {
        switch (nextIdleAnim){
            case 0 -> {
                this.anim = IdleAnim.SIT_1;
                this.level.broadcastEntityEvent(this,(byte) 4);
            }
            case 1 -> {
                this.anim = IdleAnim.SIT_2;
                this.level.broadcastEntityEvent(this,(byte) 8);
            }
            case 2 -> {
                this.anim = IdleAnim.SIT_3;
                this.level.broadcastEntityEvent(this,(byte) 60);
            }
        }
    }

    @Override
    public Block getWorkBlock() {
        return null;
    }
    public Direction getAttachFace() {
        return this.entityData.get(DATA_ATTACH_FACE_ID);
    }

    public void setAttachFace(Direction p_149789_) {
        this.entityData.set(DATA_ATTACH_FACE_ID, p_149789_);
    }

    private void tickPart(ItemPart p_31116_, double p_31117_, double p_31118_, double p_31119_) {
        p_31116_.setPos(this.getX() + p_31117_, this.getY() + p_31118_, this.getZ() + p_31119_);
    }
    private void tickPartDelta(ItemPart p_31116_, double p_31117_, double p_31118_, double p_31119_) {
        p_31116_.setPos(p_31116_.getX() + p_31117_, p_31116_.getY() + p_31118_, p_31116_.getZ() + p_31119_);
    }
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this,"controller",0, state -> {
            if(this.isCrafting()){
                state.getController().setAnimation(new AnimationBuilder().addAnimation("crafting" , ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }else {
                state.getController().setAnimation(new AnimationBuilder().addAnimation(this.anim.getAnim(), ILoopType.EDefaultLoopTypes.LOOP));
            }

            return PlayState.CONTINUE;
        }));
    }
    @Override
    public boolean canBeLeashed(Player p_21418_) {
        return false;
    }
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_21434_, DifficultyInstance p_21435_, MobSpawnType p_21436_, @Nullable SpawnGroupData p_21437_, @Nullable CompoundTag p_21438_) {
        BlockState state= BteMobsBlocks.ORIANA_OAK.get().defaultBlockState();
        this.setAttachFace(Direction.NORTH);
        BlockPos pos1=this.blockPosition();
        this.setPos(Vec3.atCenterOf(pos1.above(3)));
        if(!this.level.isClientSide){
            p_21434_.getLevel().setBlock(pos1, state.setValue(OrianaOakBlock.FACING,Direction.SOUTH),3);
            state.getBlock().setPlacedBy(p_21434_.getLevel(),pos1, state , null , ItemStack.EMPTY);
        }
        this.tablePos=pos1;
        this.createInventory();
        return super.finalizeSpawn(p_21434_, p_21435_, p_21436_, p_21437_, p_21438_);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }
    public void startCrafting(ItemStack stack){
        this.resultItem=stack;
        this.setCrafting(true);
    }
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource p_20122_) {
        return p_20122_ != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACH_FACE_ID,Direction.EAST);
        this.entityData.define(CRAFTING,false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_21484_) {
        super.addAdditionalSaveData(p_21484_);
        p_21484_.putByte("AttachFace", (byte)this.getAttachFace().get3DDataValue());
        ListTag listtag = new ListTag();
        if(this.inventory!=null){
            for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.inventory.getItem(i);
                if (!itemstack.isEmpty()) {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putByte("Slot", (byte)i);
                    itemstack.save(compoundtag);
                    listtag.add(compoundtag);
                }
            }

            p_21484_.put("Items", listtag);
        }
        if(this.tablePos!=null){
            p_21484_.putInt("x",this.tablePos.getX());
            p_21484_.putInt("y",this.tablePos.getY());
            p_21484_.putInt("z",this.tablePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_21450_) {
        super.readAdditionalSaveData(p_21450_);
        this.setAttachFace(Direction.from3DDataValue(p_21450_.getByte("AttachFace")));
        if(this.inventory==null){
            this.createInventory();
        }
        if(this.inventory!=null){
            ListTag listtag = p_21450_.getList("Items", 10);

            for(int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                int j = compoundtag.getByte("Slot") & 255;
                if (j < this.inventory.getContainerSize()) {
                    this.inventory.setItem(j, ItemStack.of(compoundtag));
                }
            }
        }
        if(p_21450_.contains("x") && p_21450_.contains("y") && p_21450_.contains("z")){
            this.tablePos = new BlockPos(p_21450_.getInt("x"),
                    p_21450_.getInt("y"),
                    p_21450_.getInt("z"));
        }
    }

    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        return super.mobInteract(pPlayer, pHand);
    }

    public boolean isCrafting() {
        return this.entityData.get(CRAFTING);
    }

    public void setCrafting(boolean p_149789_) {
        this.entityData.set(CRAFTING, p_149789_);
        this.craftingTime= p_149789_ ? 205 : 0 ;
    }
    @Override
    public void handleEntityEvent(byte p_21375_) {
        if(p_21375_==4){
            this.anim = IdleAnim.SIT_1;
            this.animIdle = 54;
        }else if(p_21375_==8){
            this.anim = IdleAnim.SIT_2;
            this.animIdle = 54;
        }else if(p_21375_==60){
            this.anim = IdleAnim.SIT_3;
            this.animIdle = 54;
        }else if(p_21375_==6){
            this.setCrafting(true);
        }else {
            super.handleEntityEvent(p_21375_);
        }
    }
    @Override
    protected BodyRotationControl createBodyControl() {
        return new ExplorerEntity.NPCBodyRotationControl(this);
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_218825_) {
        super.recreateFromPacket(p_218825_);
        if (true) return; // Forge: Fix MC-158205: Moved into setId()
        ItemPart[] part =this.items;
        for(int i = 0 ; i<part.length;i++){
            part[i].setId(i + p_218825_.getId());
        }
    }



    @Override
    public @Nullable PartEntity<?>[] getParts() {
        return this.items;
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return false;
    }


    public void clearOrSpawnItem(Player player){
        if(this.craftingItem.item != null){
            if(player.getInventory().add(this.craftingItem.item.copy())){
                this.craftingItem.item = null;
            }
        }
    }

    public void openCraftGui(ServerPlayer player){
        this.clearOrSpawnItem(player);
        this.openMenu((MenuProvider) this,player);
    }

    public OptionalInt openMenu(@javax.annotation.Nullable MenuProvider pMenu,ServerPlayer serverPlayer) {
        if (pMenu == null) {
            return OptionalInt.empty();
        } else {
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.closeContainer();
            }

            serverPlayer.nextContainerCounter();
            AbstractContainerMenu abstractcontainermenu = pMenu.createMenu(serverPlayer.containerCounter, serverPlayer.getInventory(), serverPlayer);
            if (abstractcontainermenu == null) {
                if (serverPlayer.isSpectator()) {
                    serverPlayer.displayClientMessage(Component.translatable("container.spectatorCantOpen").withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            } else {
                serverPlayer.connection.send(new ClientboundOpenScreenPacket(abstractcontainermenu.containerId, abstractcontainermenu.getType(), pMenu.getDisplayName()));
                serverPlayer.initMenu(abstractcontainermenu);
                serverPlayer.containerMenu = abstractcontainermenu;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(serverPlayer, serverPlayer.containerMenu));
                return OptionalInt.of(serverPlayer.containerCounter);
            }
        }
    }
    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new DruidMenu(pContainerId,pPlayerInventory,this.inventory,this.dataAccess);
    }
    @Override
    public int getContainerSize() {
        return 6;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return this.inventory.getItem(pSlot);
    }

    @Override
    public ItemStack removeItem(int p_58330_, int p_58331_) {
        return this.inventory.removeItem(p_58330_,p_58331_);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_58387_) {
        return this.inventory.removeItemNoUpdate(p_58387_);
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        this.inventory.setItem(pSlot,pStack);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return false;
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
    protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
        return new net.minecraftforge.items.wrapper.InvWrapper(this);
    }

    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY )
            return itemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
    }

    @Override
    public void containerChanged(Container pContainer) {

    }



    class NpcLookControl extends LookControl {
        public NpcLookControl(Mob p_149820_) {
            super(p_149820_);
        }

        protected void clampHeadRotationToBody() {
        }

        protected Optional<Float> getYRotD() {
            Direction direction = DruidEntity.this.getAttachFace().getOpposite();
            Vector3f vector3f = DruidEntity.FORWARD.copy();
            vector3f.transform(direction.getRotation());
            Vec3i vec3i = direction.getNormal();
            Vector3f vector3f1 = new Vector3f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
            vector3f1.cross(vector3f);
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedY - this.mob.getEyeY();
            double d2 = this.wantedZ - this.mob.getZ();
            Vector3f vector3f2 = new Vector3f((float)d0, (float)d1, (float)d2);
            float f = vector3f1.dot(vector3f2);
            float f1 = vector3f.dot(vector3f2);
            return !(Math.abs(f) > 1.0E-5F) && !(Math.abs(f1) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(Mth.atan2((double)(-f), (double)f1) * (double)(180F / (float)Math.PI)));
        }

        protected Optional<Float> getXRotD() {
            return Optional.of(0.0F);
        }
    }
    public enum IdleAnim{
        SIT_1("idle"),
        SIT_2("idle2"),
        SIT_3("idle3");
        private final String anim;
        IdleAnim(String anim){
            this.anim = anim;
        }

        public String getAnim(){
            return this.anim;
        }
    }
}
