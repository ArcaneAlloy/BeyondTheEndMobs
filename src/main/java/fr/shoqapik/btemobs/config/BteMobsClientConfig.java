package fr.shoqapik.btemobs.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion de cliente (config/bte_mobs-client.toml).
 * Preferencias por jugador que solo afectan a su propia experiencia.
 */
public final class BteMobsClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BLACKSMITH_SKIP_CRAFT_ANIMATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("blacksmith");
        BLACKSMITH_SKIP_CRAFT_ANIMATION = builder
                .comment("If true, the Blacksmith skips the hammering animation and the crafted item appears on the forge instantly.",
                         "Can also be toggled with the 'No Animations' checkbox in the Blacksmith crafting screen.")
                .define("skipCraftAnimation", false);
        builder.pop();

        SPEC = builder.build();
    }

    private BteMobsClientConfig() {}
}
