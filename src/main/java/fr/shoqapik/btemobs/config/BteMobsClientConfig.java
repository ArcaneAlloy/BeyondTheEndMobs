package fr.shoqapik.btemobs.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion de cliente (config/bte_mobs-client.toml).
 * Preferencias por jugador que solo afectan a su propia experiencia.
 */
public final class BteMobsClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BLACKSMITH_SKIP_CRAFT_ANIMATION;
    public static final ForgeConfigSpec.IntValue TRACKER_MAX_QUESTS;
    public static final ForgeConfigSpec.BooleanValue TRACKER_VISIBLE;
    public static final ForgeConfigSpec.IntValue TRACKER_X;
    public static final ForgeConfigSpec.IntValue TRACKER_Y;
    public static final ForgeConfigSpec.DoubleValue TRACKER_SCALE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("blacksmith");
        BLACKSMITH_SKIP_CRAFT_ANIMATION = builder
                .comment("If true, the Blacksmith skips the hammering animation and the crafted item appears on the forge instantly.",
                         "Can also be toggled with the 'No Animations' checkbox in the Blacksmith crafting screen.")
                .define("skipCraftAnimation", false);
        builder.pop();

        builder.push("quest_tracker");
        TRACKER_MAX_QUESTS = builder
                .comment("Maximum number of quests that can be tracked at the same time.")
                .defineInRange("maxTrackedQuests", 5, 1, 10);
        TRACKER_VISIBLE = builder
                .comment("Show the tracked quests on the left of the screen. Toggled in game with the 'Show/hide tracked quests' key (Ctrl+X by default).")
                .define("visible", true);
        TRACKER_X = builder
                .comment("Horizontal position of the tracker, in GUI pixels from the left edge.")
                .defineInRange("x", 4, 0, 2000);
        TRACKER_Y = builder
                .comment("Vertical position of the tracker, in GUI pixels from the top edge.")
                .defineInRange("y", 70, 0, 2000);
        TRACKER_SCALE = builder
                .comment("Text size of the tracker (1.0 = normal text size).")
                .defineInRange("scale", 0.75D, 0.5D, 1.5D);
        builder.pop();

        SPEC = builder.build();
    }

    private BteMobsClientConfig() {}
}
