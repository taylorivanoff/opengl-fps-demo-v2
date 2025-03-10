package com.example.input;

import static org.lwjgl.glfw.GLFW.*;

import com.example.components.*;

public class PlayerController {
    private int playerEntity;
    private ECSRegistry registry;

    public PlayerController(int playerEntity, ECSRegistry registry) {
        this.playerEntity = playerEntity;
        this.registry = registry;
    }

    public void update(long window) {
        TransformComponent transform = registry.getComponent(playerEntity, TransformComponent.class);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            transform.z -= 0.1f;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            transform.z += 0.1f;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            transform.x -= 0.1f;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            transform.x += 0.1f;
        }
    }
}
