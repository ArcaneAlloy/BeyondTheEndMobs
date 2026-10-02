package fr.shoqapik.btemobs.quest;

/**
 * Recompensa que desbloquea todas las recetas de los NPC que usan un ingrediente
 * (p. ej. minecraft:netherite_ingot), excepto las que ya desbloquea otra quest.
 * JSON: { "type": "UNLOCK_RECIPES_BY_INGREDIENT", "ingredient": "minecraft:netherite_ingot" }
 */
public class IngredientRecipesRewardData extends RewardData {
    public final String ingredientId;

    public IngredientRecipesRewardData(String ingredientId) {
        super(Type.UNLOCK_RECIPES_BY_INGREDIENT, 1);
        this.ingredientId = ingredientId;
    }

    @Override
    public String getObjectId() {
        return ingredientId;
    }
}
