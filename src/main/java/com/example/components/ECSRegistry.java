package com.example.components;

import java.util.*;

// ECS registry that maps entity IDs to component maps.
public class ECSRegistry {
    private Map<Integer, Map<Class<? extends Component>, Component>> entities = new HashMap<>();
    private int nextEntityId = 0;

    public int createEntity() {
        int id = nextEntityId++;
        entities.put(id, new HashMap<>());
        return id;
    }

    public <T extends Component> void addComponent(int entity, T component) {
        Map<Class<? extends Component>, Component> comps = entities.get(entity);
        if (comps != null) {
            comps.put(component.getClass(), component);
        }
    }

    public <T extends Component> T getComponent(int entity, Class<T> compClass) {
        Map<Class<? extends Component>, Component> comps = entities.get(entity);
        if (comps != null) {
            return compClass.cast(comps.get(compClass));
        }
        return null;
    }

    public Map<Integer, Map<Class<? extends Component>, Component>> getEntities() {
        return entities;
    }

    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }
}