package fr.shoqapik.btemobs.client;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.CommonEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    // Se dispara cada vez que el cliente recibe/actualiza las recetas (al entrar a un mundo,
    // y tras un /reload). Aqui reconstruimos el cache de tooltips de uso para que siempre
    // refleje las recetas bte_mobs:* reales, sin depender de ningun archivo generado a mano.
    @SubscribeEvent
    public static void onRecipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        CommonEvents.rebuildItemUsage(event.getRecipeManager());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Tooltip en libros encantados y Ancient Tomes de Quark
        boolean isEnchantedBook = stack.getItem() instanceof EnchantedBookItem;
        boolean isAncientTome = ForgeRegistries.ITEMS.getKey(stack.getItem()) != null
            && ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("quark:ancient_tome");

        if (isEnchantedBook || isAncientTome) {
            event.getToolTip().add(Component
                .translatable("bte_mobs.enchanted_book.tooltip")
                .withStyle(ChatFormatting.GREEN));
        }

        // Tooltip de tier
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        Integer tier = CommonEvents.getItemTiers().get(itemId);
        if (tier != null) {
            event.getToolTip().add(Component
                .translatable("bte_mobs.tier." + tier)
                .withStyle(CommonEvents.getTierStyle(tier)));
        }

        // Tooltip de uso: una sola linea "Used By: X, Y, Z" que agrupa los tags crudos en 4 categorias
        // (Crafting / Upgrading / Enchanting / Brewing Potions), sin importar por que NPC/receta se
        // haya obtenido el tag. Ej. Iron Sword (crafteable por Anna + upgradeable) -> "Used By: Crafting, Upgrading".
        java.util.List<String> uses = CommonEvents.getItemUsage().get(itemId);
        if (uses != null && !uses.isEmpty()) {
            java.util.LinkedHashSet<String> groups = new java.util.LinkedHashSet<>();
            for (String group : CommonEvents.getUsageDisplayGroupOrder()) {
                for (String rawTag : uses) {
                    if (group.equals(CommonEvents.getUsageDisplayGroup(rawTag))) {
                        groups.add(group);
                        break;
                    }
                }
            }
            if (!groups.isEmpty()) {
                net.minecraft.network.chat.MutableComponent line = Component
                    .translatable("bte_mobs.usage.used_by_prefix").withStyle(ChatFormatting.GRAY);
                int i = 0;
                for (String group : groups) {
                    line.append(" ").append(Component
                        .translatable(CommonEvents.getUsageGroupLangKey(group))
                        .withStyle(CommonEvents.getUsageGroupStyle(group)));
                    if (i < groups.size() - 1) {
                        line.append(Component.literal(",").withStyle(ChatFormatting.GRAY));
                    }
                    i++;
                }
                event.getToolTip().add(line);
            }
        }
    }
}
