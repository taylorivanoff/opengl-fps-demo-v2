package com.example.components;

import com.example.rendering.Mesh;

public class MeshComponent implements Component {
    public Mesh mesh;

    public MeshComponent(Mesh mesh) {
        this.mesh = mesh;
    }
}