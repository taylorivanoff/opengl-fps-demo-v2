package com.example.physics;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

public class PhysicsWorld {
    private DiscreteDynamicsWorld dynamicsWorld;

    public PhysicsWorld() {
        DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        DbvtBroadphase broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new javax.vecmath.Vector3f(0, -9.8f, 0));
    }

    public void addRigidBody(com.bulletphysics.dynamics.RigidBody body) {
        dynamicsWorld.addRigidBody(body);
    }

    public void removeRigidBody(com.bulletphysics.dynamics.RigidBody body) {
        dynamicsWorld.removeRigidBody(body);
    }

    public void stepSimulation(float dt) {
        dynamicsWorld.stepSimulation(dt, 10);
    }
}
