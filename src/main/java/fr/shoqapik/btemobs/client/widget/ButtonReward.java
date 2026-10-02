package fr.shoqapik.btemobs.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.quest.RewardData;
import fr.shoqapik.btemobs.quest.UnlockRecipeRewardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class ButtonReward extends Button {
    public RewardData data;
    public ItemStack item;
    public Minecraft minecraft;
    public ButtonReward(int p_93721_, int p_93722_, int p_93723_, int p_93724_,RewardData data,Minecraft minecraft) {
        super(p_93721_, p_93722_, p_93723_, p_93724_, Component.empty(),(button)->{},(s,p1,p2,p3)->{

        });
        this.data = data;
        this.minecraft = minecraft;
        this.item = getItemOfRewardData();

    }

    @Override
    public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
        super.render(p_93657_, p_93658_, p_93659_, p_93660_);

        if (data.type == RewardData.Type.ITEM) {
            renderItem(p_93657_);
        } else if (!getRecipeItems().isEmpty()) {
            renderStack(currentRecipeIcon(), false);   // icono del item de la receta (rota si hay varias)
        } else {
            renderTexture(p_93657_);
        }



    }

    /** El tooltip lo dibuja QuestScreen al final del frame (drawTooltip), para que nada lo tape. */
    @Override
    public void renderToolTip(PoseStack p_93736_, int p_93737_, int p_93738_) {
    }

    public void drawTooltip(PoseStack p_93736_, int p_93737_, int p_93738_) {
        // Una sola receta: tooltip del item con una línea "Desbloquea la receta:" encima
        if ((data.type == RewardData.Type.UNLOCK_RECIPE || data.type == RewardData.Type.UNLOCK_RECIPES_BY_INGREDIENT)
                && getRecipeItems().size() == 1) {
            ItemStack single = getRecipeItems().get(0);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.bte_mobs.quest.unlock_recipe").withStyle(net.minecraft.ChatFormatting.GOLD));
            lines.addAll(this.minecraft.screen.getTooltipFromItem(single));
            addRequiresLine(lines);
            if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
                lines.add(Component.translatable("gui.bte_mobs.quest.click_jei").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            this.minecraft.screen.renderComponentTooltip(p_93736_, lines, p_93737_, p_93738_, single);
            return;
        }
        // Varias recetas: tooltip corto; la lista completa se abre con clic (panel en QuestScreen)
        if (hasRecipeList()) {
            List<ItemStack> items = getRecipeItems();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.bte_mobs.quest.unlock_recipes", items.size()));
            addRequiresLine(lines);
            lines.add(Component.translatable("gui.bte_mobs.quest.click_recipes").withStyle(net.minecraft.ChatFormatting.GRAY));
            this.minecraft.screen.renderComponentTooltip(p_93736_, lines, p_93737_, p_93738_);
            return;
        }
        if (this.item.isEmpty()){
            this.minecraft.screen.renderComponentTooltip(p_93736_, getMessageForType(),p_93737_,p_93738_);
        }else {
            this.minecraft.screen.renderComponentTooltip(p_93736_, this.minecraft.screen.getTooltipFromItem(item), p_93737_, p_93738_, item);

        }

    }

    /** "Requiere también: <quest>" si la receta necesita otra quest reclamada. */
    private void addRequiresLine(List<Component> lines) {
        if (data instanceof fr.shoqapik.btemobs.quest.UnlockRecipeRewardData d && d.requiresQuest != null) {
            String path = d.requiresQuest.contains(":") ? d.requiresQuest.substring(d.requiresQuest.indexOf(':') + 1) : d.requiresQuest;
            lines.add(Component.translatable("gui.bte_mobs.quest.requires_quest",
                    Component.translatable("title.quest." + path)).withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    private List<Component> getMessageForType() {
        boolean isGroupItem = this.data.getObjectId().contains("#");
        String name = this.data.getObjectId().split(":")[1];
        switch (this.data.type){
            case UNLOCK_OPTION_DIALOG -> {
                String key = "gui.bte_mobs.quest.unlock_option." + this.data.getObjectId().replace(':', '.');
                return List.of(net.minecraft.client.resources.language.I18n.exists(key)
                        ? Component.translatable(key)
                        : Component.literal("Unlock Option Dialog " + name));
            }
            case UNLOCK_RECIPE -> {
                if (isGroupItem){

                    List<Component> list = new ArrayList<>();
                    list.add(Component.literal("Unlock Recipes :"));
                    ResourceLocation tagId = new ResourceLocation(data.getObjectId().substring(1));

                    TagKey<Item> tag = ItemTags.create(tagId);

                    IForgeRegistry<Item> itemRegistry = ForgeRegistries.ITEMS;
                    int count = 0;
                    for (ResourceLocation rl : itemRegistry.getKeys()){
                        ItemStack item = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));

                        if (item.is(tag)){
                            count++;
                            list.add(item.getDisplayName());
                        }
                        if (count>10){
                            list.add(Component.literal("more items"));
                            break;
                        }
                    }
                    return list;
                }
                return List.of(Component.literal("Unlock Recipe "+ name));
            }
            case UNLOCK_RECIPES_BY_INGREDIENT -> {
                // Ninguna receta que mostrar (p. ej. ya las desbloquea otra quest): se indica el ingrediente
                ResourceLocation ingredientId = ResourceLocation.tryParse(this.data.getObjectId());
                net.minecraft.world.item.Item ingredient = ingredientId != null ? ForgeRegistries.ITEMS.getValue(ingredientId) : null;
                Component ingredientName = ingredient != null ? new ItemStack(ingredient).getHoverName() : Component.literal(name);
                return List.of(Component.translatable("gui.bte_mobs.quest.unlock_recipes_ingredient", ingredientName));
            }
            case UNLOCK_ZONE -> {
                return List.of(Component.literal("Unlock Zone "+ name));
            }
            default -> {
                return List.of(Component.literal("Reward not implement. "));
            }
        }
    }

    private List<ItemStack> recipeItems;

    /** Items cuyas recetas desbloquea esta recompensa (tag "#mod:tag" o un item suelto). Vacío si no es UNLOCK_RECIPE. */
    public List<ItemStack> getRecipeItems() {
        if (recipeItems != null) return recipeItems;
        List<ItemStack> list = new ArrayList<>();
        if (data.type == RewardData.Type.UNLOCK_RECIPES_BY_INGREDIENT) {
            // Resultados distintos de las recetas que desbloquea (calculado con las recetas y quests que conoce el cliente)
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null && mc.player != null) {
                fr.shoqapik.btemobs.capability.RecipeCapability<?> cap = fr.shoqapik.btemobs.capability.RecipeCapability.get(mc.player);
                java.util.List<fr.shoqapik.btemobs.quest.Quest> known = new java.util.ArrayList<>();
                if (cap != null) cap.quests.values().forEach(m -> known.addAll(m.keySet()));
                java.util.Set<net.minecraft.world.item.Item> seenItems = new java.util.LinkedHashSet<>();
                for (net.minecraft.world.item.crafting.Recipe<?> r : fr.shoqapik.btemobs.quest.QuestRecipeLocks.recipesFor(
                        mc.getConnection().getRecipeManager(), known, data)) {
                    ItemStack res = r.getResultItem();
                    if (!res.isEmpty() && seenItems.add(res.getItem())) list.add(new ItemStack(res.getItem()));
                }
            }
            recipeItems = list;
            return list;
        }
        if (data.type == RewardData.Type.UNLOCK_RECIPE) {
            String id = data.getObjectId();
            if (id.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(id.substring(1));
                if (tagId != null && ForgeRegistries.ITEMS.tags() != null) {
                    ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(tagId)).forEach(i -> list.add(new ItemStack(i)));
                }
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(id);
                if (itemId != null && ForgeRegistries.ITEMS.containsKey(itemId)) list.add(new ItemStack(ForgeRegistries.ITEMS.getValue(itemId)));
            }
        }
        recipeItems = list;
        return list;
    }

    /** true si desbloquea varias recetas (se muestran en el panel que se abre con clic). */
    public boolean hasRecipeList() {
        return getRecipeItems().size() > 1;
    }

    public boolean isMouseOver(double p_93672_, double p_93673_) {
        return p_93672_ >= (double)this.x && p_93673_ >= (double)this.y && p_93672_ < (double)(this.x + this.width) && p_93673_ < (double)(this.y + this.height);
    }

    public ItemStack getItemOfRewardData(){
        ItemStack stack = ItemStack.EMPTY;
        if (data.type == RewardData.Type.ITEM){
            stack = data instanceof fr.shoqapik.btemobs.quest.ItemRewardData itemData
                    ? itemData.createStack()
                    : new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.getObjectId())),data.count);
        }
        return stack;
    }

    /** Cada cuántos milisegundos cambia el icono cuando la recompensa desbloquea varias recetas (como en JEI). */
    public static final long ICON_CYCLE_MS = 1000;

    /** Item de receta que se muestra ahora: fijo si es una sola, rotando si son varias. */
    public ItemStack currentRecipeIcon() {
        List<ItemStack> items = getRecipeItems();
        if (items.isEmpty()) return ItemStack.EMPTY;
        int index = (int) ((System.currentTimeMillis() / ICON_CYCLE_MS) % items.size());
        return items.get(index);
    }

    private void renderStack(ItemStack stack, boolean decorations) {
        Minecraft mc = Minecraft.getInstance();
        int ix = this.x + (this.width - 16) / 2;
        int iy = this.y + (this.height - 16) / 2;
        mc.getItemRenderer().renderAndDecorateItem(stack, ix, iy);
        if (decorations) mc.getItemRenderer().renderGuiItemDecorations(mc.font, stack, ix, iy);
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
