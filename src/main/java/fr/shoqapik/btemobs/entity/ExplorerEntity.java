package fr.shoqapik.btemobs.entity;

import com.mojang.math.Vector3f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.block.ExplorerTableBlock;
import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.packets.SyncPacket;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.Optional;

public class ExplorerEntity extends BteAbstractEntity implements IAnimatable {
    public final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(ExplorerEntity.class, EntityDataSerializers.DIRECTION);
    protected static final EntityDataAccessor<Boolean> CRAFTING = SynchedEntityData.defineId(ExplorerEntity.class, EntityDataSerializers.BOOLEAN);
    public int craftingTime=0;
    public int animIdle=0;
    private BlockPos tablePos = null;
    public ItemStack resultItem=ItemStack.EMPTY;
    public IdleAnim anim= IdleAnim.SIT_1;
    static final Vector3f FORWARD = Util.make(() -> {
        Vec3i vec3i = Direction.SOUTH.getNormal();
        return new Vector3f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
    });
    public ExplorerEntity(EntityType<? extends Mob> p_21368_, Level p_21369_) {
        super(p_21368_, p_21369_);
        this.lookControl = new ExplorerLookControl(this);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.animIdle>0){
            this.animIdle--;
            if(this.animIdle==0){
                if(!this.level.isClientSide){
                    boolean flag=this.random.nextBoolean();
                    this.anim=flag ? IdleAnim.SIT_1 : IdleAnim.SIT_2 ;
                    this.level.broadcastEntityEvent(this, flag ?(byte) 8 :(byte) 60 );
                }
            }
        }
        if(this.isCrafting()){
            this.craftingTime--;
            if(this.craftingTime==40){
                this.finishCrafting();
            }else if(this.craftingTime==0){
                this.setCrafting(false);
            }
        }
        if(!this.level.isClientSide){
            if(this.tablePos!=null){
                if(this.level.getBlockEntity(this.tablePos) instanceof ExplorerTableBlockEntity table){
                    if(this.getId()!=table.getData(0)){
                        table.setData(0,this.getId());
                        BteMobsMod.sendToAllTracking(new SyncPacket(this.getId(),this.tablePos),this);
                    }
                }else{
                    this.tablePos=null;
                    this.level.broadcastEntityEvent(this,(byte) 4);
                }
            }
        }
        this.setYBodyRot(this.getAttachFace().toYRot());
    }

    @Override
    public void handleEntityEvent(byte p_21375_) {
        if(p_21375_==4){
            this.tablePos=null;
        }else if(p_21375_==8){
            this.anim= IdleAnim.SIT_1;
            this.animIdle=120;
        }else if(p_21375_==60){
            this.anim= IdleAnim.SIT_2;
            this.animIdle=50;
        }else {
            super.handleEntityEvent(p_21375_);
        }
    }

    public void finishCrafting(){
        if(this.tablePos!=null && this.level.getBlockEntity(this.tablePos) instanceof ExplorerTableBlockEntity table){
            table.setItem(this.resultItem);
        }
        this.resultItem=null;
    }

    public void startCrafting(ItemStack stack){
        this.resultItem=stack;
        this.setCrafting(true);
    }

    @Override
    protected InteractionResult mobInteract(Player p_21472_, InteractionHand p_21473_) {
        if(!p_21472_.isLocalPlayer()) {
            /*if (p_21472_.getItemInHand(p_21473_).getItem() == NpcItems.NPC_WRENCH.get()) {
                if (p_21472_ instanceof ServerPlayer && p_21472_.hasPermissions(2) && p_21472_.isCreative()) {
                    NewNpcMod.sendToClient(new ShowNpcEditScreenPacket(this.getUUID(), this.getConfObject()), (ServerPlayer) p_21472_);
                }
                return InteractionResult.SUCCESS;
            } else if (getConfObject().getDialogs() != null && !getConfObject().getDialogs().isEmpty() && getConfObject().getQuest() != null && getConfObject().getQuest() != null && !getConfObject().getQuest().isEmpty()) {
                if(p_21473_ == InteractionHand.MAIN_HAND) {
                    if (PlayerQuestHelper.isQuestCompleted(getConfObject().getNpcName(), getConfObject().getQuest(), PlayerQuestHelper.getQuestDatas(p_21472_, getConfObject().getNpcName()))) {
                        PlayerQuestHelper.rewardPlayer(getConfObject().getNpcName(), p_21472_);
                    } else {
                        NewNpcMod.sendToClient(new NpcSendQuestPacket(getConfObject().getNpcName(), !PlayerQuestHelper.hasQuest(p_21472_, getConfObject().getNpcName()), getConfObject().getDialogs(), PlayerQuestHelper.getQuestDatas(p_21472_, getConfObject().getNpcName()), getConfObject().getQuest()), (ServerPlayer) p_21472_);
                    }
                }
                return InteractionResult.SUCCESS;
            }*/
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void setTablePos(BlockPos pos){
        this.tablePos=pos;
    }
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_21434_, DifficultyInstance p_21435_, MobSpawnType p_21436_, @Nullable SpawnGroupData p_21437_, @Nullable CompoundTag p_21438_) {
        if(!this.level.isClientSide){
            BlockState state= BteMobsBlocks.EXPLORER_TABLE.get().defaultBlockState();
            this.setAttachFace(Direction.SOUTH);
            BlockPos pos1=this.blockPosition().south();
            this.setTablePos(pos1);
            this.setPos(Vec3.atCenterOf(pos1.north()));
            p_21434_.getLevel().setBlock(pos1, state.setValue(ExplorerTableBlock.FACING,Direction.SOUTH),3);
            if(this.level.getBlockEntity(pos1) instanceof ExplorerTableBlockEntity tableBlock){
                tableBlock.setData(0,this.getId());
            }
            state.getBlock().setPlacedBy(p_21434_.getLevel(),pos1, state , null , ItemStack.EMPTY);
        }
        return super.finalizeSpawn(p_21434_, p_21435_, p_21436_, p_21437_, p_21438_);
    }

    public boolean isCrafting() {
        return this.entityData.get(CRAFTING);
    }

    public void setCrafting(boolean p_149789_) {
        this.entityData.set(CRAFTING, p_149789_);
        this.craftingTime=p_149789_ ? 140 : 0 ;
    }

    @Override
    public BteNpcType getNpcType() {
        return BteNpcType.EXPLORER;
    }

    @Override
    public Block getWorkBlock() {
        return null;
    }

    public Direction getAttachFace() {
        return this.entityData.get(DATA_ATTACH_FACE_ID);
    }

    private void setAttachFace(Direction p_149789_) {
        this.entityData.set(DATA_ATTACH_FACE_ID, p_149789_);
    }


    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
    @Override
    public boolean canBeLeashed(Player p_21418_) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
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
        if(p_21450_.contains("x") && p_21450_.contains("y") && p_21450_.contains("z")){
            this.tablePos = new BlockPos(p_21450_.getInt("x"),
                    p_21450_.getInt("y"),
                    p_21450_.getInt("z"));
        }
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new ExplorerBodyRotationControl(this);
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }


    public static AttributeSupplier.Builder getExplorerAttributes() {
        return Mob.createMobAttributes().add(ForgeMod.ENTITY_GRAVITY.get(), 1.5f)
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7246D);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this,"controller",0, state -> {
            boolean isMove= !(state.getLimbSwingAmount() > -0.15F && state.getLimbSwingAmount() < 0.15F);
            if(this.isCrafting()){
                state.getController().setAnimation(new AnimationBuilder().addAnimation("crafting" , ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }else {
                state.getController().setAnimation(new AnimationBuilder().addAnimation(this.anim == IdleAnim.SIT_1 ? "sit" : "sit_two", ILoopType.EDefaultLoopTypes.LOOP));
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    static class ExplorerBodyRotationControl extends BodyRotationControl {
        public ExplorerBodyRotationControl(Mob p_149816_) {
            super(p_149816_);
        }

        public void clientTick() {
        }
    }

    class ExplorerLookControl extends LookControl {
        public ExplorerLookControl(Mob p_149820_) {
            super(p_149820_);
        }

        protected void clampHeadRotationToBody() {
        }

        protected Optional<Float> getYRotD() {
            Direction direction = ExplorerEntity.this.getAttachFace().getOpposite();
            Vector3f vector3f = ExplorerEntity.FORWARD.copy();
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
        SIT_1,
        SIT_2
    }
}
