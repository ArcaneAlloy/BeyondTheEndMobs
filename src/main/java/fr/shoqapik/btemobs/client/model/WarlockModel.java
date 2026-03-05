package fr.shoqapik.btemobs.client.model;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class WarlockModel extends AnimatedGeoModel<BteAbstractEntity> {

    @Override
    public ResourceLocation getModelResource(BteAbstractEntity BteAbstractEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "geo/warlock.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BteAbstractEntity BteAbstractEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "textures/entity/warlock.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BteAbstractEntity BteAbstractEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "animations/warlock.animation.json");
    }
}
