package fr.shoqapik.btemobs.quest;

public class UnlockRecipeRewardData extends RewardData{
    public String recipeType;
    public UnlockRecipeRewardData(String id,String recipeType) {
        super(Type.UNLOCK_RECIPE, id, 1);
        this.recipeType = recipeType;
    }
}
