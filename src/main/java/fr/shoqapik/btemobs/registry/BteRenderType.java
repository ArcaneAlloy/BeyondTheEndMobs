package fr.shoqapik.btemobs.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BteRenderType extends RenderType {
    public BteRenderType(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }
    public static final RenderType ORB_RENDER_TYPE = RenderType.create(
            "orb_transparent",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            new ResourceLocation(BteMobsMod.MODID, "textures/entity/druid/orb.png"),
                            false,
                            false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY) // NO aditivo
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST) // Agregado
                    .setWriteMaskState(WriteMaskStateShard.COLOR_WRITE)
                    .createCompositeState(true)
    );
}
