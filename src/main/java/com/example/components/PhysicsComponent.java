package com.example.components;

import com.bulletphysics.dynamics.*;

public class PhysicsComponent implements Component {
    public RigidBody rigidBody;

    public PhysicsComponent(RigidBody rigidBody) {
        this.rigidBody = rigidBody;
    }
}
