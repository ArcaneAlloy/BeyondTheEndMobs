package fr.shoqapik.btemobs.entity;

import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

public class WarlockEntity extends BteAbstractEntity {

    protected static final AnimationBuilder IDLE_ANIMATION_ONE = new AnimationBuilder().addAnimation("idle_one", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
    protected static final AnimationBuilder IDLE_ANIMATION_TWO = new AnimationBuilder().addAnimation("idle_two", ILoopType.EDefaultLoopTypes.PLAY_ONCE);

    private int summonHandParticlesTick;

    public WarlockEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BteNpcType getNpcType() {
        return BteNpcType.WARLOCK;
    }

    @Override
    public Block getWorkBlock() {
        return BteMobsBlocks.MAGMA_FORGE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if(summonHandParticlesTick > 0) {
            Level level = this.level;

            float yaw = this.getYRot();
            double radians = Math.toRadians(yaw);

            double forwardOffset = 0.5;
            double rightOffset = 0.6;

            double offsetX = -Math.sin(radians) * forwardOffset - Math.cos(radians) * rightOffset;
            double offsetZ = Math.cos(radians) * forwardOffset - Math.sin(radians) * rightOffset;

            // Randomized motion
            double randomMotionX = (Math.random() - 0.5) * 0.01;
            double randomMotionY = Math.random() * 0.015;
            double randomMotionZ = (Math.random() - 0.5) * 0.01;

            level.addParticle(
                    ParticleTypes.FLAME,
                    this.getX() + offsetX,
                    this.getY() + 1.5,
                    this.getZ() + offsetZ,
                    randomMotionX,
                    randomMotionY,
                    randomMotionZ
            );

            summonHandParticlesTick++;
            if(summonHandParticlesTick == 180) {
                summonHandParticlesTick = 0;
            }
        }
    }

    protected AnimationController<? extends BteAbstractEntity> getIdleAnimationController(AnimationData animationData) {
        AnimationController<? extends BteAbstractEntity> animationController = super.getIdleAnimationController(animationData);
        animationController.registerParticleListener(event -> {
            if(event.effect.equals("hand_particle")) {
                summonHandParticlesTick = 1;
            }

        });
        return animationController;
    }

    @Override
    protected PlayState idleAnimation(AnimationEvent<BteAbstractEntity> event) {
        if(isCrafting()) {
            // TODO: event.getController().setAnimation(CRAFTING_ANIMATION);
            return PlayState.CONTINUE;
        }

        if(!event.isMoving()) {
            if (event.getController().getAnimationState() == AnimationState.Stopped) {
                if (event.getController().getCurrentAnimation() == null ||
                        event.getController().getCurrentAnimation().animationName.equals("idle_two")) {
                    event.getController().setAnimation(IDLE_ANIMATION_ONE);
                    event.getController().setAnimationSpeed(0.6D);
                } else if (event.getController().getCurrentAnimation().animationName.equals("idle_one")) {
                    event.getController().setAnimation(IDLE_ANIMATION_TWO);
                    event.getController().setAnimationSpeed(0.6D);
                }
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }
}
