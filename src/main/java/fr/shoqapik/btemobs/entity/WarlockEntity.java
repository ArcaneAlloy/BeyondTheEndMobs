package fr.shoqapik.btemobs.entity;

import fr.shoqapik.btemobs.menu.DruidMenu;
import fr.shoqapik.btemobs.menu.WarlockCraftMenu;
import fr.shoqapik.btemobs.menu.WarlockPotionMenu;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

public class WarlockEntity extends BteAbstractEntity implements WorldlyContainer, ContainerListener, MenuProvider {

    protected static final AnimationBuilder IDLE_ANIMATION_ONE = new AnimationBuilder().addAnimation("idle_one", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
    protected static final AnimationBuilder IDLE_ANIMATION_TWO = new AnimationBuilder().addAnimation("idle_two", ILoopType.EDefaultLoopTypes.PLAY_ONCE);

    private int summonHandParticlesTick;
    public SimpleContainer inventory;
    public int id;
    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            return WarlockEntity.this.getId();
        }

        public void set(int p_58433_, int p_58434_) {

        }

        public int getCount() {
            return 1;
        }
    };
    public WarlockEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.createInventory();
    }

    @Override
    public List<UUID> getInteractedPlayers() {
        return List.of();
    }

    @Override
    public BteNpcType getNpcType() {
        return BteNpcType.WARLOCK;
    }

    @Override
    public Block getWorkBlock() {
        return BteMobsBlocks.MAGMA_FORGE.get();
    }
    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(7);
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

    public void openCraftGui(ServerPlayer player){
        this.openMenu((MenuProvider) this,player);
    }

    public OptionalInt openMenu(@javax.annotation.Nullable MenuProvider pMenu, ServerPlayer serverPlayer) {
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
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new WarlockPotionMenu(pContainerId,pPlayerInventory,this.inventory,this.dataAccess);
    }
    @Override
    public int getContainerSize() {
        return 7;
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
        itemHandler = net.minecraftforge.common.util.LazyOptional.of(this::createUnSidedHandler);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
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

            pCompound.put("Items", listtag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if(this.inventory==null){
            this.createInventory();
        }
        if(this.inventory!=null){
            ListTag listtag = pCompound.getList("Items", 10);

            for(int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                int j = compoundtag.getByte("Slot") & 255;
                if (j < this.inventory.getContainerSize()) {
                    this.inventory.setItem(j, ItemStack.of(compoundtag));
                }
            }
        }
    }

    @Override
    public void containerChanged(Container pContainer) {

    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return false;
    }
}
