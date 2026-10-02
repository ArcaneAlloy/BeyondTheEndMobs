package fr.shoqapik.btemobs.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class TaskData {
    public Type type;
    public String description;
    public int count;
    private String entityIdLocation;
    public TaskData(Type type,String description,String location,int count){
        this.type = type;
        this.description = description;
        this.count = count;
        this.entityIdLocation = location;
    }

    public int getCount() {
        return count;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public String getEntityIdLocation() {
        return entityIdLocation;
    }

    public enum Type {
        HUNTER,
        COLLECT,
        EXPLORING,
        BOSS_HUNTER,
        /** Craftear N items DISTINTOS. entityIdLocation: id de item o tag ("#mod:tag"). */
        CRAFT_UNIQUE,
        /** Crear una poción en la interfaz de pociones del Warlock. entityIdLocation: solo icono. */
        BREW_POTION,
        /** Alimentar a una entidad con un item. entityIdLocation: "entidad|item". */
        FEED_ENTITY,
        /** Tener un Personal Horse de Callable Horses. entityIdLocation: solo icono. */
        PERSONAL_HORSE,
        /** Haber tenido el item en el inventario (no se entrega). entityIdLocation: id de item o tag. */
        OBTAIN,
        /** Encantar un item en la interfaz de encantamientos del Warlock. entityIdLocation: solo icono. */
        WARLOCK_ENCHANT,
        /** Craftear (o mejorar) un item en la interfaz de Anna. entityIdLocation: id de item o tag ("#mod:tag"). */
        BLACKSMITH_CRAFT,
        /**
         * Tener activada una waystone (mod Waystones) fuera del Lobby (the_forgotten_realm).
         * entityIdLocation: "item_icono" (cualquier waystone) o "item_icono|mod:estructura" (solo si está dentro de esa estructura).
         */
        ACTIVATE_WAYSTONE
    }
}
