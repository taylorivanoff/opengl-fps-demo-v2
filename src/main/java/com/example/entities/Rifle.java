package com.example.entities;

import org.joml.*;

import com.example.components.*;
import com.example.rendering.Mesh;

public class Rifle extends Weapon {
    public Rifle() {
        this.damage = 25.0f;
        this.bulletSpeed = 50.0f;
    }

    @Override
    public void fire(ECSRegistry ecs, Mesh bulletMesh, float startX, float startY, float startZ, float directionX,
            float directionY, float directionZ) {
        int bulletEntity = ecs.createEntity();

        TransformComponent bulletTransform = new TransformComponent(startX, startY, startZ);

        ecs.addComponent(bulletEntity, bulletTransform);
        ecs.addComponent(bulletEntity, new MeshComponent(bulletMesh));
        ecs.addComponent(bulletEntity, new ColliderComponent(0.2f, 0.2f, 0.2f));

        Vector3f direction = new Vector3f(directionX, directionY, directionZ).normalize();
        Vector3f velocity = new Vector3f(direction).mul(bulletSpeed);
        Vector3f acceleration = new Vector3f(0, -9.8f, 0);

        ecs.addComponent(bulletEntity, new BulletComponent(velocity, acceleration, 5.0f));
    }
}
