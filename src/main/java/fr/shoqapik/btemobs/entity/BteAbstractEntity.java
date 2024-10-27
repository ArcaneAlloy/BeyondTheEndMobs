package fr.shoqapik.btemobs.entity;

import fr.shoqapik.btemobs.blockentity.MagmaForgeBlockEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.IItemHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.Optional;

public abstract class BteAbstractEntity extends Mob implements IAnimatable {

    protected static final AnimationBuilder IDLE_ANIMATION = new AnimationBuilder().addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP);
    protected static final AnimationBuilder CRAFTING_ANIMATION = new AnimationBuilder().addAnimation("crafting", ILoopType.EDefaultLoopTypes.PLAY_ONCE);

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    private BlockPos workBlock = null;
    private boolean crafting;
    private static final EntityDataAccessor<ItemStack> CRAFT_ITEM = SynchedEntityData.defineId(BteAbstractEntity.class, EntityDataSerializers.ITEM_STACK);;

    private int animationTickCount = 0;

    public BteAbstractEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    /*@Override
    protected InteractionResult mobInteract(Player p_21472_, InteractionHand p_21473_) {
        if(!p_21472_.isLocalPlayer()) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }*/

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CRAFT_ITEM, ItemStack.EMPTY);
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
    }

    @Override
    public void tick() {
        super.tick();

        if(workBlock == null || !(this.level.getBlockState(workBlock).getBlock() == getWorkBlock())) {
            workBlock = null;
            for(Direction direction : Direction.values()) {
                BlockPos blockPos = this.blockPosition().offset(direction.getNormal());
                if(this.level.getBlockState(blockPos).getBlock() == getWorkBlock()) {
                    workBlock = blockPos;
                }
            }
        }

        if(workBlock != null) {
            this.lookAt(EntityAnchorArgument.Anchor.FEET, new Vec3(workBlock.getX(), workBlock.getY(), workBlock.getZ()));
        }

        if(isCrafting()) {
            if(!this.level.isClientSide) {
                if(animationTickCount == 54 || animationTickCount == 88 || animationTickCount == 115) {
                    ((ServerLevel)this.level).sendParticles(ParticleTypes.CRIT, workBlock.getX() + 0.5D, workBlock.getY() + 1.1D, workBlock.getZ() + 0.5D, 6, 0.1D, 0.3D, 0.1D, 0.1D);
                }
            }

            if(animationTickCount == 115) {
                BlockEntity blockEntity = this.level.getBlockEntity(this.workBlock);
                if(blockEntity instanceof MagmaForgeBlockEntity) {
                    blockEntity.getCapability(MagmaForgeBlockEntity.ITEM_HANDLER).resolve().get().insertItem(0, getCraftItem().copy(), false);
                }
                setCraftItem(ItemStack.EMPTY);
            }

            if(animationTickCount >= 173) {
                setCrafting(false);
                animationTickCount = 0;
            }
            animationTickCount++;
        }
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
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    public static AttributeSupplier.Builder getBlacksmithAttributes() {
        return Mob.createMobAttributes().add(ForgeMod.ENTITY_GRAVITY.get(), 1.5f).add(Attributes.MAX_HEALTH, 25.0D).add(Attributes.MOVEMENT_SPEED, 0.7246D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        CompoundTag itemStack = new CompoundTag();
        getCraftItem().save(itemStack);
        pCompound.put("craftItem", itemStack);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setCraftItem(ItemStack.of((CompoundTag) pCompound.get("craftItem")));
    }

    public boolean isWorkBlockEmpty() {
        if(getCraftItem().getItem() == Items.AIR) {
            if(workBlock != null) {
                BlockEntity blockEntity = this.level.getBlockEntity(workBlock);
                Optional<IItemHandler> optional = blockEntity.getCapability(MagmaForgeBlockEntity.ITEM_HANDLER).resolve();
                if(optional.isPresent()) {
                    if(optional.get().getStackInSlot(0).getItem() == Items.AIR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isCrafting() {
        return crafting;
    }

    public void setCrafting(boolean crafting) {
        this.crafting = crafting;
    }

    public ItemStack getCraftItem() {
        return this.entityData.get(CRAFT_ITEM);
    }

    public void setCraftItem(ItemStack craftItem) {
        this.entityData.set(CRAFT_ITEM, craftItem);
    }

    public abstract BteNpcType getNpcType();

    public abstract Block getWorkBlock();

    // Animation Setup

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "Idling", 5, this::idleAnimationController));
    }

    protected PlayState idleAnimationController(AnimationEvent<BteAbstractEntity> event) {
        if(isCrafting()) {
            event.getController().setAnimation(CRAFTING_ANIMATION);
            return PlayState.CONTINUE;
        }

        if(!event.isMoving()) {
            event.getController().setAnimation(IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }


    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

}