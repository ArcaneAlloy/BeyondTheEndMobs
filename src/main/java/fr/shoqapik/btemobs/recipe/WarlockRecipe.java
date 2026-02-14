package fr.shoqapik.btemobs.recipe;

import com.google.gson.*;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.menu.container.BteAbstractCraftContainer;
import fr.shoqapik.btemobs.recipe.api.BteAbstractRecipe;
import fr.shoqapik.btemobs.recipe.api.BteRecipeCategory;
import fr.shoqapik.btemobs.registry.BteMobsRecipeSerializers;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class WarlockRecipe implements Recipe<SimpleContainer> {
    protected final ResourceLocation id;
    protected final Enchantment enchantment;
    protected final ResourceLocation texture;
    protected final int level;
    protected final int experience;
    public final Ingredient requiredItem;
    public final int needEyes;
    public WarlockRecipe(ResourceLocation resourceLocation, BteRecipeCategory category, ResourceLocation enchantment, ResourceLocation texture, int level,int eyes, int experience, Ingredient ingredients) {
        this.enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantment);
        this.texture = texture == null ? new ResourceLocation("minecraft:textures/item/enchanted_book.png") : texture;
        this.level = level;
        this.needEyes = eyes;
        this.experience = experience;
        this.requiredItem = ingredients;
        this.id = resourceLocation;
    }


    public Enchantment getEnchantment() {
        return enchantment;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getLevel() {
        return level;
    }
    public int getNeedEyes(){
        return this.needEyes;
    }

    public int getExperience() {
        return this.experience;
    }

    @Override
    public ItemStack getResultItem() {
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment,level));
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    public Ingredient getRequiredItems(){
        return requiredItem;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        return hasItems(pContainer);
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer) {
        ItemStack result = pContainer.getItem(3).is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : pContainer.getItem(3).copy();
        if (result.is(Items.ENCHANTED_BOOK)){
            EnchantedBookItem.addEnchantment(result,new EnchantmentInstance(enchantment,level));
        }else {
            int oldLevel = EnchantmentHelper.getTagEnchantmentLevel(enchantment,result);

            if(oldLevel>level){
                return ItemStack.EMPTY;
            }else if(oldLevel>0){
                CompoundTag tag = getTagEnchantment(enchantment,result);
                if(tag!=null){
                    EnchantmentHelper.setEnchantmentLevel(tag,level);
                }
            }else {
                result.enchant(enchantment, level);
            }
        }

        return result;
    }
    public static CompoundTag getTagEnchantment(Enchantment pEnchantment, ItemStack pStack) {
        ResourceLocation resourcelocation = EnchantmentHelper.getEnchantmentId(pEnchantment);
        ListTag listtag = pStack.getEnchantmentTags();

        for(int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            ResourceLocation resourcelocation1 = EnchantmentHelper.getEnchantmentId(compoundtag);
            if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
                return compoundtag;
            }
        }

        return null;
    }
    public boolean hasItems(SimpleContainer container){
        int countItemValid=0;
        int slotFull = 0;
        for(int i = 0 ; i<container.getContainerSize() ; i++){
            if(!container.getItem(i).isEmpty()){
                slotFull++;
            }
        }
        for(ItemStack stack:this.requiredItem.getItems()){
            if(valid(container,stack)!=-1){
                countItemValid++;
            }
        }

        return countItemValid==this.requiredItem.getItems().length && countItemValid==slotFull;
    }
    @Override
    public NonNullList<Ingredient> getIngredients(){
        NonNullList<Ingredient> ingredients = NonNullList.create();

        for (ItemStack stack : this.requiredItem.getItems()) {
            ingredients.add(Ingredient.of(stack));
        }

        return ingredients;
    }
    public boolean hasItems(Player player){
        int countItemValid=0;
        for(ItemStack stack:this.requiredItem.getItems()){
            if(valid(player.getInventory(),stack)!=-1){
                countItemValid++;
            }
        }
        return countItemValid==this.requiredItem.getItems().length;
    }
    public int valid(SimpleContainer container, ItemStack stack1){
        for(int index = 0 ; index<container.getContainerSize() ; index++ ){
            ItemStack stack =container.getItem(index);
            if(stack1.getItem()==stack.getItem()){
                if(stack.getCount()>=stack1.getCount()){
                    return index;
                }

            }
        }
        return -1;
    }
    public int valid(Inventory inventory, ItemStack stack1){
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
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BteMobsRecipeSerializers.WARLOCK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BteMobsRecipeTypes.WARLOCK_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<WarlockRecipe> {
        @Override
        public WarlockRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String enchantId = json.get("enchantment").getAsString();
            ResourceLocation enchantment = new ResourceLocation(enchantId);
            Optional<Holder.Reference<Enchantment>> delegate =
                    ForgeRegistries.ENCHANTMENTS.getDelegate(enchantment);

            if (delegate.isEmpty()) {
                throw new JsonSyntaxException("Enchantment does not exist: " + enchantId);
            }

            ResourceLocation texture = null;
            if (json.has("texture")) {
                texture = new ResourceLocation(json.get("texture").getAsString());
            }
            int eyes = 0;
            if(json.has("eyes")){
                eyes = json.get("eyes").getAsInt();
            }
            int level = json.get("level").getAsInt();
            int experience = json.get("experience").getAsInt();
            Ingredient ingredients = fromJson(GsonHelper.getAsJsonArray(json, "ingredients"));

            return new WarlockRecipe(recipeId,BteRecipeCategory.ALL,enchantment,texture,level,eyes,experience,ingredients);
        }

        @Override
        public WarlockRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer) {
            ResourceLocation enchantment = pBuffer.readResourceLocation();
            ResourceLocation texture = pBuffer.readResourceLocation();
            int level = pBuffer.readInt();
            int experience = pBuffer.readInt();
            int eyes = pBuffer.readInt();
            Ingredient ingredients = Ingredient.fromNetwork(pBuffer);
            return new WarlockRecipe(recipeId,BteRecipeCategory.ALL,enchantment,texture,level,eyes,experience,ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, WarlockRecipe recipe) {
            ResourceLocation location = ForgeRegistries.ENCHANTMENTS.getKey(recipe.enchantment);
            if(location==null){
                throw new IllegalStateException("Network");
            }
            pBuffer.writeResourceLocation(location);
            pBuffer.writeResourceLocation(recipe.texture);
            pBuffer.writeInt(recipe.level);
            pBuffer.writeInt(recipe.experience);
            pBuffer.writeInt(recipe.needEyes);
            recipe.getRequiredItems().toNetwork(pBuffer);
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

    }
}
