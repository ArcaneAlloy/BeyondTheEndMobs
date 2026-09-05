package fr.shoqapik.btemobs.quest;

public class UnlockRecipeRewardData extends RewardData{
    public String recipeType;
    public String recipeId;
    public UnlockRecipeRewardData(String id,String recipeType) {
        super(Type.UNLOCK_RECIPE, 1);
        this.recipeType = recipeType;
        this.recipeId = id;
    }

    @Override
    public String getObjectId() {
        return recipeId;
    }
}
