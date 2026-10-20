package fr.shoqapik.btemobs.integration;

import fr.shoqapik.btemobs.BteMobsMod;

import fr.shoqapik.btemobs.integration.druid_create_jei.DruidCraftCategory;
import fr.shoqapik.btemobs.integration.warlock_enchant_jei.WarlockEnchantCategory;
import fr.shoqapik.btemobs.integration.warlock_potion_jei.WarlockPotionCategory;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;


import java.util.List;
import java.util.Objects;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static RecipeType<DruidRecipe> DRUID_CRAFT_TYPE =
            new RecipeType<>(DruidCraftCategory.UID, DruidRecipe.class);
    public static RecipeType<WarlockPotionRecipe> WARLOCK_POTION_TYPE =
            new RecipeType<>(WarlockPotionCategory.UID, WarlockPotionRecipe.class);
    public static RecipeType<WarlockRecipe> WARLOCK_ENCHANT_TYPE =
            new RecipeType<>(WarlockEnchantCategory.UID, WarlockRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BteMobsMod.MODID, "jei_plugin");
    }

    private static mezz.jei.api.runtime.IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /**
     * Abre en JEI las recetas que producen este item (como pulsar R sobre él).
     * Solo se debe llamar si JEI está cargado (ModList.get().isLoaded("jei")).
     */
    public static boolean showRecipesFor(net.minecraft.world.item.ItemStack stack) {
        if (runtime == null || stack.isEmpty()) return false;
        mezz.jei.api.recipe.IFocus<net.minecraft.world.item.ItemStack> focus = runtime.getJeiHelpers().getFocusFactory()
                .createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
        runtime.getRecipesGui().show(focus);
        return true;
    }

    /**
     * Las interfaces de crafteo de los NPCs tienen panel de recetas y pestanas fuera de su imageWidth; sin esto los
     * favoritos y la lista de ingredientes de JEI se dibujaban encima.
     */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        addExtraAreas(registration, fr.shoqapik.btemobs.client.gui.BlacksmithCraftScreen.class);
        addExtraAreas(registration, fr.shoqapik.btemobs.client.gui.ExplorerTableScreen.class);
        addExtraAreas(registration, fr.shoqapik.btemobs.client.gui.DruidScreen.class);
        addExtraAreas(registration, fr.shoqapik.btemobs.client.gui.WarlockCraftScreen.class);
        addExtraAreas(registration, fr.shoqapik.btemobs.client.gui.WarlockPotionCraftScreen.class);
    }

    private static <T extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>
    void addExtraAreas(mezz.jei.api.registration.IGuiHandlerRegistration registration, Class<T> screenClass) {
        registration.addGuiContainerHandler(screenClass, new mezz.jei.api.gui.handlers.IGuiContainerHandler<T>() {
            @Override
            public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(T screen) {
                return screen instanceof fr.shoqapik.btemobs.client.gui.JeiExtraAreas areas ? areas.getJeiExtraAreas() : List.of();
            }
        });
        // Ademas le decimos a JEI que la interfaz empieza en el borde izquierdo de las pestanas: los favoritos
        // (a la izquierda) y la lista de items (a la derecha) se colocan fuera de toda la interfaz, no solo
        // fuera del panel de inventario.
        registration.addGuiScreenHandler(screenClass, JEIPlugin::fullGuiProperties);
    }

    private static mezz.jei.api.gui.handlers.IGuiProperties fullGuiProperties(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int right = left + screen.getXSize();
        int bottom = top + screen.getYSize();
        if (screen instanceof fr.shoqapik.btemobs.client.gui.JeiExtraAreas areas) {
            for (net.minecraft.client.renderer.Rect2i r : areas.getJeiExtraAreas()) {
                left = Math.min(left, r.getX());
                top = Math.min(top, r.getY());
                right = Math.max(right, r.getX() + r.getWidth());
                bottom = Math.max(bottom, r.getY() + r.getHeight());
            }
        }
        final int guiLeft = left, guiTop = top, xSize = right - left, ySize = bottom - top;
        final int screenWidth = screen.width, screenHeight = screen.height;
        final Class<? extends net.minecraft.client.gui.screens.Screen> screenClass = screen.getClass();
        return new mezz.jei.api.gui.handlers.IGuiProperties() {
            @Override public Class<? extends net.minecraft.client.gui.screens.Screen> getScreenClass() { return screenClass; }
            @Override public int getGuiLeft() { return guiLeft; }
            @Override public int getGuiTop() { return guiTop; }
            @Override public int getGuiXSize() { return xSize; }
            @Override public int getGuiYSize() { return ySize; }
            @Override public int getScreenWidth() { return screenWidth; }
            @Override public int getScreenHeight() { return screenHeight; }
        };
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DruidCraftCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WarlockPotionCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new WarlockEnchantCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager rm = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        List<DruidRecipe> recipeDruid = rm.getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
        List<WarlockRecipe> recipeWarlock = rm.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());
        List<WarlockPotionRecipe> recipeWarlockPotion = rm.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get());

        if (!recipeDruid.isEmpty()) {
            registration.addRecipes(JEIPlugin.DRUID_CRAFT_TYPE, recipeDruid);
        }
        // Each recipe JSON is already a specific variant (base / long / strong / splash / lingering).
        // No need to generate permutations here — JEI shows each recipe as-is.
        if (!recipeWarlockPotion.isEmpty()) {
            registration.addRecipes(JEIPlugin.WARLOCK_POTION_TYPE, recipeWarlockPotion);
        }
        if (!recipeWarlock.isEmpty()) {
            registration.addRecipes(JEIPlugin.WARLOCK_ENCHANT_TYPE, recipeWarlock);
        }
    }
}
