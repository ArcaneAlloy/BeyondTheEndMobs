package fr.shoqapik.btemobs.client.model;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.WarlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class WarlockModel extends AnimatedGeoModel<WarlockEntity> {

    @Override
    public ResourceLocation getModelResource(WarlockEntity warlockEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "geo/warlock.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WarlockEntity warlockEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "textures/entity/warlock.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WarlockEntity warlockEntity) {
        return new ResourceLocation(BteMobsMod.MODID, "animations/warlock.animation.json");
    }
}
