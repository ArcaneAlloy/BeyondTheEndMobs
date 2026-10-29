package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Vector3f;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.compendium.PageCompendium;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.NotNull;
import software.bernie.shadowed.eliotlash.mclib.math.functions.limit.Min;

public class EntityPage {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(BteMobsMod.MODID, "textures/gui/entity_page.png");
    final ResourceLocation bg;

    final EntityType<?> type;
    /**
     * Entidad que se dibuja. Se crea una sola vez (antes se creaba una nueva en cada frame) y con todos los giros a
     * 0: LivingEntity nace con un giro aleatorio en la cabeza (yHeadRot), así que cada frame la cabeza apuntaba a
     * otro sitio y temblaba (p. ej. el caballo).
     */
    private Entity entity;
    public <T extends Entity> EntityPage() {
        this.type = null;
        this.bg = BACKGROUND;
    }
    public <T extends Entity> EntityPage(EntityType<T> type) {
        this.type = type;
        this.bg = BACKGROUND;
    }

    public ResourceLocation getBg() {
        return bg;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PageCompendium page, PoseStack mStack, int x, int y, int mouseX, int mouseY) {
        Entity e = getEntity();
        if (e == null) return;

        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);

        Tesselator tess = Tesselator.getInstance();

        mStack.pushPose();
        float scale = (e.getBbHeight());
        y -= -3+3*scale;
        scale = 1.0F/scale;
        scale = 50 * scale;

        mStack.translate(x + 64, y + 136, 64);
        mStack.mulPose(Vector3f.XP.rotationDegrees(-15));
        mStack.mulPose(Vector3f.YP.rotationDegrees(page.getExtraYRot()-30.0F));

        mStack.scale(scale, -scale, scale);
        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(tess.getBuilder());
        Lighting.setupForFlatItems();
        renderer.render(e, e.getYRot(), 0, mStack, buf, 0xf000f0);
        buf.endLastBatch();
        Lighting.setupFor3DItems();
        mStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private Entity getEntity() {
        if (entity == null && type != null && Minecraft.getInstance().level != null) {
            entity = type.create(Minecraft.getInstance().level);
            if (entity != null) {
                entity.setYRot(0.0F);
                entity.yRotO = 0.0F;
                entity.setXRot(0.0F);
                entity.xRotO = 0.0F;
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.yBodyRot = 0.0F;
                    living.yBodyRotO = 0.0F;
                    living.yHeadRot = 0.0F;
                    living.yHeadRotO = 0.0F;
                }
            }
        }
        return entity;
    }

    @OnlyIn(Dist.CLIENT)
    public void renderBackground(PoseStack mStack, int x, int y, int mouseX, int mouseY) {
        GuiComponent.blit(mStack, x, y, 0, 0,256, 256, 256,256);
    }
}
