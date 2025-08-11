package fr.shoqapik.btemobs.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.client.model.ExplorerModel;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.ExtendedGeoEntityRenderer;

import java.util.Objects;

public class ExplorerEntityRenderer<T extends ExplorerEntity> extends ExtendedGeoEntityRenderer<T> {
    public ExplorerEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ExplorerModel<>());
        //this.addLayer(new ChairLayer<>(this));
    }

    @Override
    protected boolean isArmorBone(GeoBone bone) {
        return false;
    }

    @Override
    public void renderChildBones(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderChildBones(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected RenderType getRenderTypeForBone(GeoBone bone, T animatable, float partialTick, PoseStack poseStack, VertexConsumer buffer, MultiBufferSource bufferSource, int packedLight, ResourceLocation texture) {
        return bone.getName().equals("chair") ? RenderType.entityCutoutNoCull(texture) : super.getRenderTypeForBone(bone, animatable, partialTick, poseStack, buffer, bufferSource, packedLight, texture);
    }

    @Nullable
    @Override
    protected ResourceLocation getTextureForBone(String boneName, T animatable) {
        return boneName.equals("chair") ? new ResourceLocation(BteMobsMod.MODID,"textures/entity/explorer/chair.png") : null;
    }

    @Nullable
    @Override
    protected ItemStack getHeldItemForBone(String boneName, T animatable) {
        return Objects.equals(boneName, "craftingItem") && animatable.craftingTime<55 && animatable.craftingTime>35 ? animatable.resultItem : null;
    }

    @Override
    protected ItemTransforms.TransformType getCameraTransformForItemAtBone(ItemStack stack, String boneName) {
        return ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
    }

    @Nullable
    @Override
    protected BlockState getHeldBlockForBone(String boneName, T animatable) {
        return null;
    }

    @Override
    protected void preRenderItem(PoseStack stack, ItemStack item, String boneName, T animatable, IBone bone) {
        stack.translate(0.0F,-0.35F,0.0F);
    }

    @Override
    protected void preRenderBlock(PoseStack poseStack, BlockState state, String boneName, T animatable) {

    }

    @Override
    protected void postRenderItem(PoseStack poseStack, ItemStack stack, String boneName, T animatable, IBone bone) {

    }

    @Override
    protected void postRenderBlock(PoseStack poseStack, BlockState state, String boneName, T animatable) {

    }
}
