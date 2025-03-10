package com.example.entities;

import org.joml.*;

import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.example.components.*;
import com.example.physics.*;
import com.example.rendering.Mesh;

public class Rifle extends Weapon {
    public Rifle() {
        this.damage = 25.0f;
        this.bulletSpeed = 50.0f;
    }

    @Override
    public void fire(ECSRegistry ecs, PhysicsWorld physicsWorld, Mesh bulletMesh, float startX, float startY,
            float startZ, float directionX,
            float directionY, float directionZ) {
        // Create a collision shape for a spherical bullet.
        SphereShape shape = new SphereShape(0.2f);

        // Set up the initial transform.
        Transform startTransform = new Transform();
        startTransform.setIdentity();
        startTransform.origin.set(startX, startY, startZ);

        DefaultMotionState motionState = new DefaultMotionState(startTransform);

        float mass = 1.0f;
        javax.vecmath.Vector3f inertia = new javax.vecmath.Vector3f(0, 0, 0);
        shape.calculateLocalInertia(mass, inertia);

        com.bulletphysics.dynamics.RigidBodyConstructionInfo rbInfo = new com.bulletphysics.dynamics.RigidBodyConstructionInfo(
                mass, motionState, shape, inertia);
        com.bulletphysics.dynamics.RigidBody body = new com.bulletphysics.dynamics.RigidBody(rbInfo);

        // Set the bullet's initial velocity.
        Vector3f dir = new Vector3f(directionX, directionY, directionZ).normalize();
        body.setLinearVelocity(
                new javax.vecmath.Vector3f(dir.x * bulletSpeed, dir.y * bulletSpeed, dir.z * bulletSpeed));

        // Add the body to your physics world.
        physicsWorld.addRigidBody(body);

        // Create an ECS bullet entity and attach components.
        int bulletEntity = ecs.createEntity();
        TransformComponent bulletTransform = new TransformComponent(startX, startY, startZ);
        ecs.addComponent(bulletEntity, bulletTransform);
        ecs.addComponent(bulletEntity, new MeshComponent(bulletMesh));
        ecs.addComponent(bulletEntity, new PhysicsComponent(body));

        // Optionally, add a collider component if needed.
        ecs.addComponent(bulletEntity, new ColliderComponent(0.2f, 0.2f, 0.2f));

        System.out.println("Bullet fired with JBullet physics!");
    }
}
