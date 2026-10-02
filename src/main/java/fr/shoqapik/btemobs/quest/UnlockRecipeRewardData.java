package fr.shoqapik.btemobs.quest;

public class UnlockRecipeRewardData extends RewardData{
    public String recipeType;
    public String recipeId;
    /**
     * Opcional ("requiresQuest" en el JSON): id de otra quest que también tiene que estar reclamada.
     * La receta se desbloquea cuando están reclamadas las dos, sea cual sea el orden.
     */
    public String requiresQuest;

    public UnlockRecipeRewardData(String id,String recipeType) {
        this(id, recipeType, null);
    }

    public UnlockRecipeRewardData(String id,String recipeType,String requiresQuest) {
        super(Type.UNLOCK_RECIPE, 1);
        this.recipeType = recipeType;
        this.recipeId = id;
        this.requiresQuest = requiresQuest == null || requiresQuest.isEmpty() ? null : requiresQuest;
    }

    @Override
    public String getObjectId() {
        return recipeId;
    }
}
