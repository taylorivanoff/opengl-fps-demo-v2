package com.example.components;

public class ExplosionComponent implements Component {
    public float lifetime;
    public float maxLifetime;

    public ExplosionComponent(float lifetime) {
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
    }
}