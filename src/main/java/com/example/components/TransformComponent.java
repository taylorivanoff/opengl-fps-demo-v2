package com.example.components;

public class TransformComponent implements Component {
    public float x, y, z;
    public float rotationX, rotationY, rotationZ;

    public TransformComponent(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotationX = 0;
        this.rotationY = 0;
        this.rotationZ = 0;
    }
}
