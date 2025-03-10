package com.example.entities;

import com.example.components.*;
import com.example.physics.*;
import com.example.rendering.*;

public abstract class Weapon {
    protected float damage;
    protected float bulletSpeed;

    public abstract void fire(ECSRegistry ecs, PhysicsWorld physicsWorld, Mesh bulletMesh,
            float startX, float startY, float startZ,
            float directionX, float directionY, float directionZ);
}