package com.example.physics;

import javax.vecmath.*;

import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.dynamics.constraintsolver.*;

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

    public boolean raycast(Vector3f from, Vector3f to) {
        CollisionWorld.ClosestRayResultCallback callback = new CollisionWorld.ClosestRayResultCallback(from, to);

        dynamicsWorld.rayTest(from, to, callback);

        return callback.hasHit();
    }
}
