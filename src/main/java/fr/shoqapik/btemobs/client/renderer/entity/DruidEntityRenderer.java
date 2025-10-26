package fr.shoqapik.btemobs.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import fr.shoqapik.btemobs.client.model.DruidModel;
import fr.shoqapik.btemobs.client.model.OrbModel;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ItemPart;
import fr.shoqapik.btemobs.registry.BteRenderType;
import mc.duzo.ender_journey.EndersJourney;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class DruidEntityRenderer extends GeoEntityRenderer<DruidEntity> {

    public final ItemRenderer itemRenderer;
    public final OrbModel<Entity> orbModel;
    public DruidEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new DruidModel());
        this.itemRenderer = context.getItemRenderer();
        this.orbModel = new OrbModel<>(context.bakeLayer(OrbModel.LAYER_LOCATION));
    }

    @Override
    public void render(GeoModel model, DruidEntity druidEntity, float partialTick, RenderType type, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.render(model, druidEntity, partialTick, type, poseStack, bufferSource, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        for(ItemPart part:druidEntity.items){
            if(part.item!=null){
                this.renderItemPart(part,druidEntity,poseStack,partialTick,bufferSource,packedLight);
            }
        }
    }
    
    public void renderItemPart(ItemPart itemPart, DruidEntity druidEntity, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, int packedLight){
        poseStack.pushPose();
        double x = Mth.lerp(partialTick,itemPart.xo,itemPart.getX()) - druidEntity.getX();
        double y = Mth.lerp(partialTick,itemPart.yo,itemPart.getY()) - druidEntity.getY();
        double z = Mth.lerp(partialTick,itemPart.zo,itemPart.getZ()) - druidEntity.getZ();


        float porcent = itemPart.isSampleItem ? 0.384615F : (itemPart.getCreationTime(partialTick)/25.0F)*0.384615F;
        poseStack.translate(x,y+0.1F,z);
        poseStack.scale(porcent,porcent,porcent);
        poseStack.pushPose();
        poseStack.scale(0.5F,0.5f,0.5f);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(5.0F*itemPart.tickCount));
        BakedModel bakedmodel = this.itemRenderer.getModel(itemPart.getItem(),null, null , 0);
        this.render(itemPart.getItem(), ItemTransforms.TransformType.FIXED,false,poseStack,bufferSource,packedLight, OverlayTexture.NO_OVERLAY,bakedmodel);
        poseStack.popPose();

        poseStack.popPose();
    }

    public void render(ItemStack pItemStack, ItemTransforms.TransformType pTransformType, boolean pLeftHand, PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, int pCombinedOverlay, BakedModel pModel) {
        if (!pItemStack.isEmpty()) {
            pPoseStack.pushPose();

            pModel = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(pPoseStack, pModel, pTransformType, pLeftHand);
            pPoseStack.translate(-0.5D, -0.5D, -0.5D);
            if (!pModel.isCustomRenderer()) {
                boolean flag1 = true;
                for (var model : pModel.getRenderPasses(pItemStack, flag1)) {
                    for (var rendertype : model.getRenderTypes(pItemStack, flag1)) {
                        VertexConsumer vertexconsumer =  pBuffer.getBuffer(Sheets.translucentItemSheet());

                        this.itemRenderer.renderModelLists(model, pItemStack, pCombinedLight, pCombinedOverlay, pPoseStack, vertexconsumer);
                    }
                }
            } else {
                net.minecraftforge.client.extensions.common.IClientItemExtensions.of(pItemStack).getCustomRenderer().renderByItem(pItemStack, pTransformType, pPoseStack, pBuffer, pCombinedLight, pCombinedOverlay);
            }

            pPoseStack.popPose();
        }
    }
}
