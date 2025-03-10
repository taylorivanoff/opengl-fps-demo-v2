package com.example.components;

public class ColliderComponent implements Component {
    public float width, height, depth;

    public ColliderComponent(float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }
}
