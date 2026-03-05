package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BteMobsEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BteMobsMod.MODID);

    public static final RegistryObject<EntityType<BlacksmithEntity>> BLACKSMITH_ENTITY = ENTITIES.register("blacksmith",
            () -> EntityType.Builder.of(BlacksmithEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).fireImmune().updateInterval(1).build(BteMobsMod.MODID + ":blacksmith"));

    public static final RegistryObject<EntityType<WarlockEntity>> WARLOCK_ENTITY = ENTITIES.register("warlock",
            () -> EntityType.Builder.of(WarlockEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).fireImmune().updateInterval(1).build(BteMobsMod.MODID + ":warlock"));

    public static final RegistryObject<EntityType<ExplorerEntity>> EXPLORER_ENTITY = ENTITIES.register("explorer",
            () -> EntityType.Builder.of(ExplorerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).fireImmune().updateInterval(1).build(BteMobsMod.MODID + ":explorer"));


    public static final RegistryObject<EntityType<DruidEntity>> DRUID_ENTITY = ENTITIES.register("druid",
            () -> EntityType.Builder.of(DruidEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).fireImmune().updateInterval(1).build(BteMobsMod.MODID + ":druid"));

    public static final RegistryObject<EntityType<Npc5Entity>> NPC5_ENTITY = ENTITIES.register("npc5",
            () -> EntityType.Builder.of(Npc5Entity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).fireImmune().updateInterval(1).build(BteMobsMod.MODID + ":npc5"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
