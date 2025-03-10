package com.example.components;

import org.joml.*;

public class BulletComponent implements Component {
    public Vector3f velocity;
    public Vector3f acceleration;
    public float lifeTime; // seconds

    public BulletComponent(Vector3f velocity, Vector3f acceleration, float lifeTime) {
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.lifeTime = lifeTime;
    }
}
