package fr.shoqapik.btemobs;

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

    public static final ResourceKey<Level> FORGOTTEN_REALM = ResourceKey.create(
            Registry.DIMENSION_REGISTRY,
            new ResourceLocation("ender_journey", "the_forgotten_realm")
    );

    private static final ResourceLocation SACRED_PLACE_ID =
            new ResourceLocation("protectyourstructures", "sacred_place");

    private static final int DURATION_TICKS = 100;
    private static final int REFRESH_THRESHOLD = 40;
    private static final int SPEED_AMP = 1;
    private static final int LOBBY_RADIUS = 500;

    private SacredPlaceHandler() {}

    private static boolean isInLobby(Player player) {
        double x = player.getX();
        double z = player.getZ();
        return x * x + z * z <= (double)(LOBBY_RADIUS * LOBBY_RADIUS);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        Player player = e.player;
        Level lvl = player.level;
        if (lvl.isClientSide) return;
        if (!lvl.dimension().equals(FORGOTTEN_REALM)) return;

        if (isInLobby(player)) {
            MobEffect sacred = ForgeRegistries.MOB_EFFECTS.getValue(SACRED_PLACE_ID);
            if (sacred != null) ensureEffect(player, sacred, 0);
            ensureEffect(player, MobEffects.MOVEMENT_SPEED, SPEED_AMP);
            ensureEffect(player, MobEffects.DAMAGE_RESISTANCE, 99);
            ensureEffect(player, MobEffects.SATURATION, 99);
        } else {
            removeLobbyEffects(player);
        }
    }

    @SubscribeEvent
    public static void onDimChange(PlayerChangedDimensionEvent e) {
        if (e.getTo().equals(FORGOTTEN_REALM)) return;

        removeLobbyEffects(e.getEntity());
    }

    /**
     * Quita los efectos del Lobby al salir de él. Solo los que pone este handler (ambientales y sin partículas, ver
     * ensureEffect): antes se quitaban Velocidad, Resistencia y Saturación vinieran de donde vinieran, así que el
     * jugador perdía las pociones que se hubiera tomado (o la Resistencia de cualquier otro mod) al salir del Lobby, y
     * en el Realm fuera del Lobby se le quitaban en cada tick. Un faro también da efectos ambientales, pero con
     * partículas, así que tampoco se toca.
     */
    private static void removeLobbyEffects(Player player) {
        MobEffect sacred = ForgeRegistries.MOB_EFFECTS.getValue(SACRED_PLACE_ID);
        if (sacred != null) removeIfLobby(player, sacred);
        removeIfLobby(player, MobEffects.MOVEMENT_SPEED);
        removeIfLobby(player, MobEffects.DAMAGE_RESISTANCE);
        removeIfLobby(player, MobEffects.SATURATION);
    }

    private static void removeIfLobby(Player player, MobEffect effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && isLobbyEffect(current)) player.removeEffect(effect);
    }

    /** ¿Lo ha puesto el Lobby? ensureEffect los crea ambientales y sin partículas; las pociones no son ambientales. */
    private static boolean isLobbyEffect(MobEffectInstance instance) {
        return instance.isAmbient() && !instance.isVisible();
    }

    private static void ensureEffect(Player player, MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        // Si el jugador ya tiene ese efecto por su cuenta (una poción...) igual o más fuerte, se le deja el suyo
        if (current != null && !isLobbyEffect(current) && current.getAmplifier() >= amplifier) return;
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() <= REFRESH_THRESHOLD) {
            player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, amplifier, true, false, true));
        }
    }
}
