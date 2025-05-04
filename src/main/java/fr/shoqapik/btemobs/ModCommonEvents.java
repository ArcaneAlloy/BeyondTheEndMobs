package fr.shoqapik.btemobs;

import fr.shoqapik.btemobs.entity.BteAbstractEntity;
import fr.shoqapik.btemobs.recipe.BteIngredientSerializer;
import fr.shoqapik.btemobs.registry.BteMobsEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(BteMobsEntities.BLACKSMITH_ENTITY.get(), BteAbstractEntity.getBlacksmithAttributes().build());
        event.put(BteMobsEntities.WARLOCK_ENTITY.get(), BteAbstractEntity.getWarlockAttributes().build());
        event.put(BteMobsEntities.EXPLORER_ENTITY.get(), BteAbstractEntity.getExplorerAttributes().build());
        event.put(BteMobsEntities.DRUID_ENTITY.get(), BteAbstractEntity.getDruidAttributes().build());
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            CraftingHelper.register(new ResourceLocation(BteMobsMod.MODID, "item"), BteIngredientSerializer.INSTANCE);
        }
    }




}
