package fr.shoqapik.btemobs.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ExplorerTableBlockRenderer<T extends ExplorerTableBlockEntity> implements BlockEntityRenderer<T> {
    public final ItemRenderer renderer;
    public ExplorerTableBlockRenderer(BlockEntityRendererProvider.Context context){
        this.renderer=context.getItemRenderer();
    }


    @Override
    public void render(T p_112307_, float p_112308_, PoseStack p_112309_, MultiBufferSource p_112310_, int p_112311_, int p_112312_) {
        assert Minecraft.getInstance().player!=null;
        if(!p_112307_.getItem().isEmpty()){
            p_112309_.pushPose();

            p_112309_.translate(0.5F,0.85F,0.5F);
            p_112309_.scale(0.5F,0.5F,0.5F);
            Quaternion rotation = Vector3f.XP.rotation((float)(Math.PI / 2));
            p_112309_.mulPose(rotation);

            this.renderer.renderStatic(p_112307_.getItem(), ItemTransforms.TransformType.FIXED,p_112311_, OverlayTexture.NO_OVERLAY,p_112309_,p_112310_,0);
            p_112309_.popPose();
        }
    }
}
