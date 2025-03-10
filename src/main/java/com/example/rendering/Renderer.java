package com.example.rendering;

import java.util.*;

import org.joml.*;
import static org.lwjgl.opengl.GL11.*;

import com.example.components.*;
import com.example.entities.*;

public class Renderer {
    private ShaderProgram shader;
    private Camera camera;
    private ECSRegistry ecs;

    public Renderer(ShaderProgram shader, Camera camera, ECSRegistry ecs) {
        this.shader = shader;
        this.camera = camera;
        this.ecs = ecs;
    }

    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use();
        camera.updateView();
        shader.setUniformMat4("view", camera.view);
        shader.setUniformMat4("projection", camera.projection);

        // Renders entities by iterating over all entities that have Transform and Mesh
        // components.
        for (Map<Class<? extends Component>, Component> comps : ecs.getEntities().values()) {
            TransformComponent transform = (TransformComponent) comps.get(TransformComponent.class);
            MeshComponent meshComp = (MeshComponent) comps.get(MeshComponent.class);

            if (transform != null && meshComp != null) {
                Matrix4f model = new Matrix4f()
                        .translation(transform.x, transform.y, transform.z)
                        .rotateXYZ(transform.rotationX, transform.rotationY, transform.rotationZ);

                // For explosion entities, it scales the model and sets alpha based on remaining
                // lifetime.
                float alpha = 1.0f;
                if (comps.containsKey(ExplosionComponent.class)) {
                    ExplosionComponent explosion = (ExplosionComponent) comps.get(ExplosionComponent.class);
                    float scaleFactor = 1.0f + (explosion.maxLifetime - explosion.lifetime) * 2.0f;
                    model.scale(scaleFactor);
                    alpha = explosion.lifetime / explosion.maxLifetime;
                }

                shader.setUniformMat4("model", model);
                shader.setUniform1f("alpha", alpha);

                meshComp.mesh.draw();
            }
        }
    }
}
