package com.example.components;

public class UIComponent implements Component {
    // VAO ID and the number of vertices to draw
    public int vaoId;
    public int vertexCount;

    // You can add additional parameters like color, transformation, etc.
    public UIComponent(int vaoId, int vertexCount) {
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }
}
