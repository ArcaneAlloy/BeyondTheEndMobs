package fr.shoqapik.btemobs.registry;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.BlacksmithCraftMenu;
import fr.shoqapik.btemobs.menu.BlacksmithRepairMenu;
import fr.shoqapik.btemobs.menu.container.explorer_menu.TableExplorerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BteMobsContainers {

    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BteMobsMod.MODID);

    public static final RegistryObject<MenuType<BlacksmithCraftMenu>> BLACKSMITH_CRAFT_MENU = CONTAINERS
            .register("blacksmith_craft", () -> new MenuType<>((id, inventory) -> new BlacksmithCraftMenu(id, inventory, 0)));
    public static final RegistryObject<MenuType<BlacksmithRepairMenu>> BLACKSMITH_REPAIR_MENU = CONTAINERS
            .register("blacksmith_repair", () -> new MenuType<>((id, inventory) -> new BlacksmithRepairMenu(id, inventory)));

    public static final RegistryObject<MenuType<TableExplorerMenu>> EXPLORER_TABLE_MENU = registerMenuType(TableExplorerMenu::new,"explorer_table_menu");

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(IContainerFactory<T> factory,
                                                                                                  String name) {
        return CONTAINERS.register(name, () -> IForgeMenuType.create(factory));
    }
    public static void register(IEventBus bus) {
        CONTAINERS.register(bus);
    }
}
