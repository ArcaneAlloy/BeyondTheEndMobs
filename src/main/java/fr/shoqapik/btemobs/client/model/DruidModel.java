package fr.shoqapik.btemobs.client.model;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.DruidEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class DruidModel extends AnimatedGeoModel<DruidEntity> {

    @Override
    public ResourceLocation getModelResource(DruidEntity druidEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "geo/oriana.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DruidEntity druidEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "textures/entity/druid/druid.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DruidEntity druidEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "animations/oriana.animation.json");
    }
}
