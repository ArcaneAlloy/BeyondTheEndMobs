package fr.shoqapik.btemobs.recipe;

import com.google.gson.*;
import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class WarlockPotionRecipe implements Recipe<SimpleContainer> {
    private final BteRecipeCategory category;
    protected final ResourceLocation id;
    private final int tier;
    private final String effect;
    private final ItemStack ingredientPrimary;
    private final ItemStack result=PotionUtils.setPotion(new ItemStack(Items.POTION), new Potion(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100,1)));
    public WarlockPotionRecipe(ItemStack stack,ResourceLocation id,String effect, int tier) {
        this.id=id;
        this.ingredientPrimary = stack;
        this.tier=tier;
        this.effect=effect;
        this.category= BteRecipeCategory.ALL;
    }

    public int getTier() {
        return tier;
    }


    public ItemStack getIngredientPrimary() {
        return ingredientPrimary;
    }

    public BteRecipeCategory getCategory() {
        return category;
    }

    @Override
    public boolean matches(SimpleContainer p_44002_, Level p_44003_) {
        ItemStack bottle = p_44002_.getItem(2);
        return bottle.is(Items.GLASS_BOTTLE) && p_44002_.getItem(3).is(ingredientPrimary.getItem());
    }

    @Override
    public ItemStack assemble(SimpleContainer p_44001_) {
        Item potion = Items.POTION;
        ItemStack mixIngredient = p_44001_.getItem(0).isEmpty() ? p_44001_.getItem(1) : p_44001_.getItem(0) ;
        if(p_44001_.getItem(4).is(Items.GUNPOWDER)){
            potion = Items.SPLASH_POTION;
        }else if(p_44001_.getItem(5).is(Items.DRAGON_BREATH)){
            potion = Items.LINGERING_POTION;
        }
        return PotionBrewing.mix(mixIngredient,PotionUtils.setPotion(new ItemStack(potion),Registry.POTION.get(new ResourceLocation(effect))));
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    public boolean hasItems(Player player,SimpleContainer craftContainer){
        Inventory inventory = player.getInventory();
        ItemStack potion = getPotion(inventory);
        return (inventory.contains(ingredientPrimary) || craftContainer.hasAnyMatching(e->e.getItem()==ingredientPrimary.getItem())) && (potion!=null || getPotion(craftContainer)!=null);
    }

    public ItemStack getPotion(Container inventory){
        ItemStack potion = null;
        for(int i=0 ; i<inventory.getContainerSize(); i++){
            if(PotionUtils.getPotion(inventory.getItem(i))==Potions.WATER){
                potion=inventory.getItem(i);
                break;
            }
        }
        return potion;
    }
    @Override
    public ItemStack getResultItem() {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), Registry.POTION.get(new ResourceLocation(effect.split(":")[0],effect.split(":")[1])));
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.WARLOCK_POTION_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<WarlockPotionRecipe> {
        public WarlockPotionRecipe fromJson(ResourceLocation p_44562_, JsonObject p_44563_) {
            String effect = GsonHelper.getAsString(p_44563_,"potion_effect");
            int tier = GsonHelper.getAsInt(p_44563_,"tier");
            ItemStack ingredient = getItemForJson(p_44563_,"ingredient");
            return new WarlockPotionRecipe(ingredient,p_44562_,effect,tier);
        }




        private ItemStack getItemForJson(JsonObject p_44563_,String name) {
            if (!p_44563_.has(name)) throw new JsonSyntaxException("Missing result, expected to find a string or object");
            ItemStack itemstack;
            if (p_44563_.get(name).isJsonObject()) itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(p_44563_, name));
            else {
                String s1 = GsonHelper.getAsString(p_44563_, name);
                ResourceLocation resourcelocation = new ResourceLocation(s1);
                itemstack = new ItemStack(ForgeRegistries.ITEMS.getDelegate(resourcelocation).orElseThrow(() -> {
                    return new IllegalStateException("Item: " + s1 + " does not exist");
                }));
            }
            return itemstack;
        }


        public WarlockPotionRecipe fromNetwork(ResourceLocation p_44565_, FriendlyByteBuf p_44566_) {
            String effect = p_44566_.readUtf();
            int tier = p_44566_.readInt();
            ItemStack result = p_44566_.readItem();
            return new WarlockPotionRecipe(result,p_44565_,effect,tier);
        }

        public void toNetwork(FriendlyByteBuf p_44553_, WarlockPotionRecipe p_44554_) {
            p_44553_.writeUtf(p_44554_.effect);
            p_44553_.writeInt(p_44554_.tier);
            p_44553_.writeItem(p_44554_.ingredientPrimary);
        }
    }
}
