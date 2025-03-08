package fr.shoqapik.btemobs.recipe;

import com.google.gson.*;
import fr.shoqapik.btemobs.blockentity.ExplorerTableBlockEntity;
import fr.shoqapik.btemobs.recipe.api.RecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class ExplorerRecipe implements Recipe<ExplorerTableBlockEntity> {
    private final RecipeCategory category;
    protected final ResourceLocation id;
    private final int tier;
    private final Ingredient requiredItems;
    private final ItemStack craftedItem;

    public ExplorerRecipe(ResourceLocation id, ItemStack craftedItem, Ingredient ingredient, int tier) {
        this.id=id;
        this.craftedItem = craftedItem;
        this.requiredItems=ingredient;
        this.tier=tier;
        this.category=RecipeCategory.ALL;
    }

    public int getTier() {
        return tier;
    }

    public Ingredient getRequiredItems() {
        return requiredItems;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public boolean hasItems(Player player){
        int countItemValid=0;
        for(ItemStack stack:this.requiredItems.getItems()){
            if(valid(player.getInventory(),stack)!=-1){
                countItemValid++;
            }
        }
        return countItemValid==this.requiredItems.getItems().length;
    }

    @Override
    public boolean matches(ExplorerTableBlockEntity p_44002_, Level p_44003_) {
        if(p_44002_.items.isEmpty()){
            return false;
        }
        int countItemValid=0;
        int index=0;
        for(ItemStack stack:p_44002_.items){
            if(isValid(stack)){
                countItemValid++;
            }
            index++;
            if(index>=p_44002_.getContainerSize()){
                break;
            }
        }
        return countItemValid==this.requiredItems.getItems().length;
    }
    public boolean isValid(ItemStack stack){
        for (ItemStack stack1 : this.requiredItems.getItems()){
            if(stack1.getItem()==stack.getItem()){
                return stack.getCount()==stack1.getCount();
            }
        }
        return false;
    }

    public int valid(Inventory inventory,ItemStack stack1){
        int index=0;
        for(ItemStack stack : inventory.items){
            if(stack1.getItem()==stack.getItem()){
                if(stack.getCount()>=stack1.getCount()){
                    return index;
                }

            }
            index++;
        }
        return -1;
    }

    @Override
    public ItemStack assemble(ExplorerTableBlockEntity p_44001_) {
        p_44001_.clearContent();
        return this.craftedItem;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return this.craftedItem;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.EXPLORER_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ExplorerRecipe> {
        public ExplorerRecipe fromJson(ResourceLocation p_44562_, JsonObject p_44563_) {
            Ingredient ingredients = fromJson(GsonHelper.getAsJsonArray(p_44563_, "ingredients"));
            ItemStack result = getItemForJson(p_44563_,"result");
            int tier =GsonHelper.getAsInt(p_44563_,"tier");
            return new ExplorerRecipe(p_44562_,result, ingredients,tier);
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

        public static Ingredient fromJson(@Nullable JsonElement p_43918_) {
            if (p_43918_ != null && !p_43918_.isJsonNull()) {
                if (p_43918_.isJsonObject()) {
                    return Ingredient.fromValues(Stream.of(valueFromJson(p_43918_.getAsJsonObject())));
                } else if (p_43918_.isJsonArray()) {
                    JsonArray jsonarray = p_43918_.getAsJsonArray();
                    if (jsonarray.size() == 0) {
                        throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
                    } else {
                        return Ingredient.fromValues(StreamSupport.stream(jsonarray.spliterator(), false).map((p_151264_) -> {
                            return valueFromJson(GsonHelper.convertToJsonObject(p_151264_, "item"));
                        }));
                    }
                } else {
                    throw new JsonSyntaxException("Expected item to be object or array of objects");
                }
            } else {
                throw new JsonSyntaxException("Item cannot be null");
            }
        }

        public static Ingredient.Value valueFromJson(JsonObject p_43920_) {
            if (p_43920_.has("item") && p_43920_.has("tag")) {
                throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
            } else if (p_43920_.has("item")) {
                ItemStack item = ShapedRecipe.itemStackFromJson(p_43920_);
                return new Ingredient.ItemValue(item);
            } else if (p_43920_.has("tag")) {
                ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_43920_, "tag"));
                TagKey<Item> tagkey = TagKey.create(Registry.ITEM_REGISTRY, resourcelocation);
                return new Ingredient.TagValue(tagkey);
            } else {
                throw new JsonParseException("An ingredient entry needs either a tag or an item");
            }
        }

        public ExplorerRecipe fromNetwork(ResourceLocation p_44565_, FriendlyByteBuf p_44566_) {
            Ingredient ingredients = Ingredient.fromNetwork(p_44566_);
            int tier = p_44566_.readInt();
            ItemStack result = p_44566_.readItem();
            return new ExplorerRecipe(p_44565_,result,ingredients,tier);
        }

        public void toNetwork(FriendlyByteBuf p_44553_, ExplorerRecipe p_44554_) {
            p_44553_.writeInt(p_44554_.tier);
            p_44554_.requiredItems.toNetwork(p_44553_);
            p_44553_.writeItem(p_44554_.craftedItem);
        }
    }
}
