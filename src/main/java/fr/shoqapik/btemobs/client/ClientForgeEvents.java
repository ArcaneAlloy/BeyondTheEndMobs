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
        if (tier == null) return;
        event.getToolTip().add(Component
            .translatable("bte_mobs.tier." + tier)
            .withStyle(CommonEvents.getTierStyle(tier)));
    }
}
