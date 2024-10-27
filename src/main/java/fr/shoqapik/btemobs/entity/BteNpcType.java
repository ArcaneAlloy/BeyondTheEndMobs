package fr.shoqapik.btemobs.entity;

public enum BteNpcType {
    BLACKSMITH(BlacksmithEntity.class);

    private final Class<? extends BteAbstractEntity> entityClass;

    BteNpcType(Class<? extends BteAbstractEntity> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends BteAbstractEntity> getEntityClass() {
        return entityClass;
    }
}