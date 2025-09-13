package fr.shoqapik.btemobs;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantType(Component name, Enchantment enchantment) {
}
