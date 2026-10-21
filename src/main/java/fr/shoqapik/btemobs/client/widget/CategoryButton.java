package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * @author shoqapik
 */
public class CategoryButton extends SmithStateSwitchingButton{
    private static final ResourceLocation CRAFTING_TABLE_LOCATION = new ResourceLocation(BteMobsMod.MODID, "textures/gui/container/explorer_screen.png");

    /**
     * Pixeles que se le quitan a lo ancho a la parte visible de las pestanas (de las de todas las interfaces de crafteo
     * de los NPCs, tambien las vanilla de Anna, ver RecipeBookTabButtonMixin). La pestana se dibuja mas a la derecha y
     * se recortan columnas del relleno, que es uniforme a partir de la 5; asi deja mas sitio a los favoritos de JEI.
     */
    public static final int SHRINK = 8;
    /** Columnas del borde izquierdo del sprite de la pestana que se conservan. */
    public static final int LEFT_EDGE = 5;

    private BteRecipeCategory category;
    /** Icono propio de la pantalla; si es null se usa el de la categoria. */
    private final ItemStack icon;
    /** Ajuste horizontal del icono (para iconos estrechos que se ven descentrados). */
    private int iconOffsetX = 0;

    public CategoryButton(BteRecipeCategory category) {
        this(category, null);
    }

    public CategoryButton(BteRecipeCategory category, ItemStack icon) {
        super(0, 0, 35, 27, false);
        this.category = category;
        this.icon = icon;
        this.initTextureValues(88, 180, 35, 0, CRAFTING_TABLE_LOCATION);
    }

    @Override
    public void renderButton(PoseStack p_94631_, int p_94632_, int p_94633_, float p_94634_) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        RenderSystem.disableDepthTest();
        int i = this.xTexStart;
        int j = this.yTexStart;
        if (this.isStateTriggered) {
            i += this.xDiffTex;
        }

        if (this.isHoveredOrFocused()) {
            j += this.yDiffTex;
        }

        int k = this.x;
        if (this.isStateTriggered) {
            k -= 2;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Borde izquierdo y, a continuacion, el resto del sprite saltandose SHRINK columnas del relleno
        blit(p_94631_, k + SHRINK, this.y, i, j, LEFT_EDGE, this.height, 512,512);
        blit(p_94631_, k + SHRINK + LEFT_EDGE, this.y, i + LEFT_EDGE + SHRINK, j, this.width - LEFT_EDGE - SHRINK, this.height, 512,512);
        RenderSystem.enableDepthTest();
        this.renderIcon(minecraft.getItemRenderer());

    }

    private void renderIcon(ItemRenderer p_100454_) {
        int i = this.isStateTriggered ? -2 : 0;
        p_100454_.renderAndDecorateFakeItem(this.icon != null ? this.icon : category.getIcon(), this.x + 9 + SHRINK / 2 + i + this.iconOffsetX, this.y + 5);
    }

    public CategoryButton setIconOffsetX(int offset) {
        this.iconOffsetX = offset;
        return this;
    }

    public BteRecipeCategory getCategory() {
        return category;
    }
}
