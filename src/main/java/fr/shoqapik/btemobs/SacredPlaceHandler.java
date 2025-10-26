package fr.shoqapik.btemobs;

import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SacredPlaceHandler {


    private static final ResourceLocation SACRED_PLACE_ID =
            new ResourceLocation("protectyourstructures", "sacred_place");

    private static final int DURATION_TICKS = 100;
    private static final int REFRESH_THRESHOLD = 40;
    private static final int SPEED_AMP = 1;

    private SacredPlaceHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        Player player = e.player;
        Level lvl = player.level;
        if (lvl.isClientSide) return;

        if (!lvl.dimension().equals(EnderDimensions.REALM_KEY)) return;

        MobEffect sacred = ForgeRegistries.MOB_EFFECTS.getValue(SACRED_PLACE_ID);
        if (sacred != null) ensureEffect(player, sacred, 0);
            ensureEffect(player, MobEffects.MOVEMENT_SPEED, SPEED_AMP);
            ensureEffect(player, MobEffects.DAMAGE_RESISTANCE, 99);
            ensureEffect(player, MobEffects.SATURATION, 99);
}

    @SubscribeEvent
    public static void onDimChange(PlayerChangedDimensionEvent e) {
        if (e.getTo().equals(EnderDimensions.REALM_KEY)) return;

        MobEffect sacred = ForgeRegistries.MOB_EFFECTS.getValue(SACRED_PLACE_ID);
        if (sacred != null) e.getEntity().removeEffect(sacred);
            e.getEntity().removeEffect(MobEffects.MOVEMENT_SPEED);
            e.getEntity().removeEffect(MobEffects.DAMAGE_RESISTANCE);
            e.getEntity().removeEffect(MobEffects.SATURATION);
        }

    private static void ensureEffect(Player player, MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() <= REFRESH_THRESHOLD) {
            player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, amplifier, true, false, true));
        }
    }
}
