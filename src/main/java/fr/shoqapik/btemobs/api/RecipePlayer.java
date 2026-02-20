package fr.shoqapik.btemobs.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.List;
import java.util.Map;

public interface RecipePlayer <T extends Recipe<?>> extends INBTSerializable<CompoundTag> {
    Player getPlayer();

    void setPlayer(Player player);

    void tick(Player player);

    void onJoinGame(Player player, EntityJoinLevelEvent event);

    void setRecipesForType(RecipeType<T> type,List<T> recipes);

    void addRecipeForType(RecipeType<T> type,T recipe);

    Map<RecipeType<T>, List<T>> getRecipeManager();

    List<T> getRecipesForType(RecipeType<T> type);

    void init(Player player, Level level);

}
