package fr.shoqapik.btemobs.entity;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.packets.ActionPacket;
import fr.shoqapik.btemobs.packets.PartItemPacket;
import fr.shoqapik.btemobs.packets.StartCraftingItemPacket;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ItemPart extends PartEntity<DruidEntity> {
    public ItemStack item = null;
    public final String name;
    private EntityDimensions size;

    private int creationTime0 = 0;
    private int creationTime = 0;
    public boolean craftingFinalize = false;
    public boolean isSampleItem;
    public int sampleTime = 0;
    public int maxSampleTime = 0;
    public ItemPart(String name,DruidEntity parent,float sizeX, float sizeY) {
        super(parent);
        this.name = name;
        this.setSize(EntityDimensions.fixed(sizeX,sizeY));
        this.isSampleItem = false;
    }
    public ItemPart(String name,DruidEntity parent,float sizeX, float sizeY,boolean isFlag) {
        super(parent);
        this.name = name;
        this.setSize(EntityDimensions.fixed(sizeX,sizeY));
        this.isSampleItem = isFlag;
        if(!this.level.isClientSide && isFlag){
            this.selectSampleItem();
        }
        this.maxSampleTime = 200;
    }
    protected void setSize(EntityDimensions size) {
        this.size = size;
        this.refreshDimensions();
    }
    @Override
    protected void defineSynchedData() {

    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;
        this.creationTime0 = this.creationTime;
        if(!this.craftingFinalize && !this.isSampleItem){
            this.creationTime++;
            if(this.creationTime==25){
                this.craftingFinalize=true;
            }

            if(this.level.isClientSide){
                Random random1 = new Random();
                double x = this.getX()+random1.nextFloat(-0.25F,0.25F);
                double y = this.getY()+0.25F+random1.nextFloat(-0.25F,0.25F);
                double z = this.getZ()+random1.nextFloat(-0.25F,0.25F);
                Vec3 delta = this.position().subtract(new Vec3(x,y,z)).normalize().scale(0.01F);
                Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.DRAGON_BREATH,x,y,z,delta.x,delta.y,delta.z);
                if(particle!=null){
                    particle.scale(0.1F);
                    particle.setColor(1.0F,0.99F,0.56F);
                }
            }
        }
        if(isSampleItem){
            this.sampleTime++;
            if(this.sampleTime>this.maxSampleTime){
                if(!level.isClientSide){
                    this.selectSampleItem();
                }
                this.sampleTime=0;
            }
            //this.setPos(this.getX(),this.getY()*Mth.cos(this.tickCount*30)*0.25f,this.getZ());
        }
    }

    private void selectSampleItem() {
        Optional<DruidRecipe> recipe;
        List<DruidRecipe> listRecipe = this.level.getRecipeManager().getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
        if(listRecipe.isEmpty())return;
        recipe= Optional.of(listRecipe.get(this.level.random.nextInt(0,listRecipe.size())));

        recipe.ifPresent(druidRecipe -> {
            this.item = druidRecipe.getResultItem();
            int extra = (int) (50*this.level.random.nextFloat());
            this.maxSampleTime = 50+extra;
            BteMobsMod.sendToAllTracking(new PartItemPacket(druidRecipe.getResultItem(),this.getParent().getId(),extra ,this.name),this.getParent());
        });

    }

    public float getCreationTime(float partialTick){
        return this.craftingFinalize ? 25.0F : Mth.lerp(partialTick,this.creationTime0,this.creationTime);
    }

    public void startCreationItem(ItemStack stack){
        this.creationTime=0;
        this.creationTime0=0;
        this.craftingFinalize=false;
        this.item=stack;
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.item = ItemStack.of(pCompound);
        this.isSampleItem = pCompound.getBoolean("isSample");
        this.craftingFinalize = pCompound.getBoolean("craftingFinalize");
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        if(this.item!=null){
            this.item.save(pCompound);
        }
        pCompound.putBoolean("isSample",this.isSampleItem);
        pCompound.putBoolean("craftingFinalize",this.craftingFinalize);
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        if(this.item!=null && !this.isSampleItem){
            if(pPlayer.getInventory().add(this.item.copy())){
                this.item=null;
            }
        }
        return super.interactAt(pPlayer, pVec, pHand);
    }

    public boolean isPickable() {
        return true;
    }
    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        return super.interact(pPlayer, pHand);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean shouldBeSaved() {
        return false;
    }
    public EntityDimensions getDimensions(Pose p_19975_) {
        return this.size;
    }
}
