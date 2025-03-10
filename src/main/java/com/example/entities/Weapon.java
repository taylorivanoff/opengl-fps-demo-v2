package com.example.entities;

import com.example.components.*;
import com.example.rendering.*;

public abstract class Weapon {
    protected float damage;
    protected float bulletSpeed;

    public abstract void fire(ECSRegistry ecs, Mesh bulletMesh,
            float startX, float startY, float startZ,
            float directionX, float directionY, float directionZ);
}