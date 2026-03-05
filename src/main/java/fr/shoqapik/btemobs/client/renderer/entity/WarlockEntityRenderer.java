package fr.shoqapik.btemobs.client.renderer.entity;

import fr.shoqapik.btemobs.client.model.WarlockModel;
import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.entity.WarlockEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class WarlockEntityRenderer extends GeoEntityRenderer<BteAbstractEntity> {

    public WarlockEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new WarlockModel());
    }

}
