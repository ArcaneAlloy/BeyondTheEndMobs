package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.capability.StatTaskData;
import fr.shoqapik.btemobs.quest.TaskData;
import fr.shoqapik.btemobs.quest.TaskData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ButtonTask extends Button {
    public StatTaskData data;
    public ItemStack item;
    public Minecraft minecraft;
    /** Texto ya traducido para el tooltip; si es null se usa data.description. */
    public String displayDescription;
    /** Ancho máximo del tooltip de la tarea antes de partir en varias líneas. */
    public static final int TOOLTIP_WIDTH = 160;
    public ButtonTask(int p_93721_, int p_93722_, int p_93723_, int p_93724_, StatTaskData data, Minecraft minecraft) {
        super(p_93721_, p_93722_, p_93723_, p_93724_, Component.empty(),(button)->{},(s,p1,p2,p3)->{

        });
        this.data = data;
        this.minecraft = minecraft;
        this.item = getItemOfTaskData();

    }

    @Override
    public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
        super.render(p_93657_, p_93658_, p_93659_, p_93660_);
        if (hasItemList()) {
            renderStack(currentListIcon());   // icono rotando entre los items del tag (como en JEI)
        } else if (!item.isEmpty()) {
            renderItem(p_93657_);
        } else {
            renderTexture(p_93657_);
        }
    }

    /** El tooltip lo dibuja QuestScreen al final del frame (drawTooltip), para que nada lo tape. */
    @Override
    public void renderToolTip(PoseStack p_93736_, int p_93737_, int p_93738_) {
    }

    public void drawTooltip(PoseStack p_93736_, int p_93737_, int p_93738_) {
        // Solo las tareas COLLECT muestran el tooltip del item; el resto (incluidas las especiales) muestran su descripción
        if (this.item.isEmpty() || data.taskType != TaskData.Type.COLLECT){
            String text = this.displayDescription != null ? this.displayDescription : this.data.description;
            List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>(this.minecraft.font.split(Component.literal(text), TOOLTIP_WIDTH));
            if (hasItemList()) {
                lines.add(Component.translatable("gui.bte_mobs.quest.click_list").withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText());
            }
            this.minecraft.screen.renderTooltip(p_93736_, lines, p_93737_, p_93738_);
        }else {
            this.minecraft.screen.renderComponentTooltip(p_93736_, this.minecraft.screen.getTooltipFromItem(item), p_93737_, p_93738_, item);
        }

    }

    private List<Component> getMessageForType() {
        return List.of(Component.literal(this.displayDescription != null ? this.displayDescription : this.data.description));
    }

    private List<ItemStack> listItems;

    /** Items del tag de la tarea ("#mod:tag"). Vacío si la tarea apunta a un solo item. */
    public List<ItemStack> getListItems() {
        if (listItems != null) return listItems;
        List<ItemStack> list = new java.util.ArrayList<>();
        if (data.id != null && data.id.startsWith("#") && data.taskType != TaskData.Type.FEED_ENTITY) {
            ResourceLocation tagId = ResourceLocation.tryParse(data.id.substring(1));
            if (tagId != null && ForgeRegistries.ITEMS.tags() != null) {
                ForgeRegistries.ITEMS.tags().getTag(net.minecraft.tags.ItemTags.create(tagId)).forEach(i -> list.add(new ItemStack(i)));
            }
        }
        listItems = list;
        return list;
    }

    /** true si la tarea acepta varios items (se listan en el panel que se abre con clic). */
    public boolean hasItemList() {
        return getListItems().size() > 1;
    }

    /** Ids de los items ya contados para esta tarea (tareas CRAFT_UNIQUE). */
    public java.util.Set<String> getDoneIds() {
        return data.seen;
    }

    private ItemStack currentListIcon() {
        List<ItemStack> items = getListItems();
        return items.get((int) ((System.currentTimeMillis() / ButtonReward.ICON_CYCLE_MS) % items.size()));
    }

    private void renderStack(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderAndDecorateItem(stack, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
    }

    public boolean isMouseOver(double p_93672_, double p_93673_) {
        return p_93672_ >= (double)this.x && p_93673_ >= (double)this.y && p_93672_ < (double)(this.x + this.width) && p_93673_ < (double)(this.y + this.height);
    }

    public ItemStack getItemOfTaskData(){
        // Icono de la tarea: para las de matar se usa la textura por defecto
        String spec = switch (data.taskType) {
            case COLLECT, CRAFT_UNIQUE, BREW_POTION, PERSONAL_HORSE, OBTAIN, WARLOCK_ENCHANT, BLACKSMITH_CRAFT -> data.id;
            case FEED_ENTITY -> data.id.contains("|") ? data.id.substring(data.id.indexOf('|') + 1) : "";
            case ACTIVATE_WAYSTONE -> data.id.contains("|") ? data.id.substring(0, data.id.indexOf('|')) : data.id;
            default -> "";
        };
        return iconFor(spec);
    }

    /** "mod:item" -> ese item; "#mod:tag" -> primer item del tag. */
    private static ItemStack iconFor(String spec) {
        if (spec == null || spec.isEmpty()) return ItemStack.EMPTY;
        if (spec.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(spec.substring(1));
            if (tagId == null || ForgeRegistries.ITEMS.tags() == null) return ItemStack.EMPTY;
            return ForgeRegistries.ITEMS.tags().getTag(net.minecraft.tags.ItemTags.create(tagId)).stream()
                    .findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return ItemStack.EMPTY;
        return new ItemStack(ForgeRegistries.ITEMS.getValue(id));
    }

    private void renderItem(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();

        ItemRenderer renderer = mc.getItemRenderer();
        ItemStack stack = item;

        // El botón puede ser más grande que el item (16x16): lo centramos dentro
        int ix = this.x + (this.width - 16) / 2;
        int iy = this.y + (this.height - 16) / 2;
        renderer.renderAndDecorateItem(stack, ix, iy);
        renderer.renderGuiItemDecorations(mc.font, stack, ix, iy);
    }

    private void renderTexture(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, new ResourceLocation(BteMobsMod.MODID,"textures/gui/buttons/warlock/open_craft.png"));

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GuiComponent.blit(poseStack, this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2, 0, 0, 16, 16, 16, 16);
    }
}
