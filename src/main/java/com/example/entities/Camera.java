package com.example.entities;

import java.lang.Math;

import org.joml.*;

public class Camera {
    public Vector3f position;
    public Vector3f target;
    public Vector3f up;
    public Matrix4f projection;
    public Matrix4f view;

    public Camera(float fov, float aspect, float near, float far) {
        position = new Vector3f(0, 0, 0);
        target = new Vector3f(0, 0, -1);
        up = new Vector3f(0, 1, 0);
        projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far);
        view = new Matrix4f();
    }

    public void updateView() {
        view.identity().lookAt(position, new Vector3f(position).add(target), up);
    }
}
