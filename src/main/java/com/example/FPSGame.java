package com.example;

import java.util.*;

import org.joml.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.example.components.*;
import com.example.entities.*;
import com.example.input.*;
import com.example.rendering.*;

public class FPSGame {
    private long window;
    private ECSRegistry ecs;
    private PlayerController playerController;
    private Renderer renderer;
    private Weapon weapon;
    private Camera camera;
    private ShaderProgram shader;
    private Mesh bulletMesh;
    private Mesh cubeMesh;
    private int playerEntity;
    private float lastFrameTime;

    private void init() throws Exception {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(1280, 720, "Java FPS", NULL, NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ecs = new ECSRegistry();

        // Create player entity (used for input and camera)
        playerEntity = ecs.createEntity();
        ecs.addComponent(playerEntity, new TransformComponent(0.0f, 0.0f, 3.0f));
        ecs.addComponent(playerEntity, new ColliderComponent(1.0f, 1.0f, 1.0f));
        ecs.addComponent(playerEntity, new HealthComponent(100));
        playerController = new PlayerController(playerEntity, ecs);

        // Create a cube entity as a static scene object
        cubeMesh = createCubeMesh();
        int cubeEntity = ecs.createEntity();
        ecs.addComponent(cubeEntity, new TransformComponent(0.0f, 0.0f, -5.0f));
        ecs.addComponent(cubeEntity, new MeshComponent(cubeMesh));
        ecs.addComponent(cubeEntity, new ColliderComponent(1.0f, 1.0f, 1.0f));
        ecs.addComponent(cubeEntity, new HealthComponent(100));

        // Create a shared bullet mesh.
        bulletMesh = createCubeMesh();

        // Shader sources (vertex shader passes position and color; fragment shader uses
        // a uniform "alpha")
        String vertexShaderSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 aPos;\n" +
                "layout (location = 1) in vec3 aColor;\n" +
                "out vec3 vertexColor;\n" +
                "uniform mat4 model;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "void main() {\n" +
                "    vertexColor = aColor;\n" +
                "    gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                "}\n";
        String fragmentShaderSource = "#version 330 core\n" +
                "in vec3 vertexColor;\n" +
                "out vec4 FragColor;\n" +
                "uniform float alpha;\n" +
                "void main() {\n" +
                "    FragColor = vec4(vertexColor, alpha);\n" +
                "}\n";
        shader = new ShaderProgram(vertexShaderSource, fragmentShaderSource);

        camera = new Camera(70f, 1280f / 720f, 0.1f, 100f);

        TransformComponent playerTransform = ecs.getComponent(playerEntity, TransformComponent.class);

        camera.position.set(playerTransform.x, playerTransform.y, playerTransform.z);
        camera.target.set(0, 0, -1);

        renderer = new Renderer(shader, camera, ecs);
        weapon = new Rifle();

        lastFrameTime = (float) glfwGetTime();
    }

    private Mesh createCubeMesh() {
        float[] vertices = {
                // positions // colors
                -0.5f, -0.5f, -0.5f, 1f, 0f, 0f,
                0.5f, -0.5f, -0.5f, 0f, 1f, 0f,
                0.5f, 0.5f, -0.5f, 0f, 0f, 1f,
                -0.5f, 0.5f, -0.5f, 1f, 1f, 0f,
                -0.5f, -0.5f, 0.5f, 1f, 0f, 1f,
                0.5f, -0.5f, 0.5f, 0f, 1f, 1f,
                0.5f, 0.5f, 0.5f, 1f, 1f, 1f,
                -0.5f, 0.5f, 0.5f, 0f, 0f, 0f,
        };
        int[] indices = {
                // back face
                0, 1, 2, 2, 3, 0,
                // front face
                4, 5, 6, 6, 7, 4,
                // left face
                4, 0, 3, 3, 7, 4,
                // right face
                1, 5, 6, 6, 2, 1,
                // bottom face
                4, 5, 1, 1, 0, 4,
                // top face
                3, 2, 6, 6, 7, 3
        };
        return new Mesh(vertices, indices);
    }

    private void updateBullets(float dt) {
        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            int id = entry.getKey();

            Map<Class<? extends Component>, Component> comps = entry.getValue();

            if (comps.containsKey(BulletComponent.class) && comps.containsKey(TransformComponent.class)) {
                BulletComponent bullet = (BulletComponent) comps.get(BulletComponent.class);
                TransformComponent transform = (TransformComponent) comps.get(TransformComponent.class);

                bullet.velocity.add(new Vector3f(bullet.acceleration).mul(dt));

                transform.x += bullet.velocity.x * dt;
                transform.y += bullet.velocity.y * dt;
                transform.z += bullet.velocity.z * dt;

                bullet.lifeTime -= dt;

                if (bullet.lifeTime <= 0) {
                    toRemove.add(id);
                }
            }
        }

        for (int id : toRemove) {
            ecs.removeEntity(id);
        }
    }

    private boolean checkCollision(TransformComponent t1, ColliderComponent c1, TransformComponent t2,
            ColliderComponent c2) {
        float t1MinX = t1.x - c1.width / 2, t1MaxX = t1.x + c1.width / 2;
        float t1MinY = t1.y - c1.height / 2, t1MaxY = t1.y + c1.height / 2;
        float t1MinZ = t1.z - c1.depth / 2, t1MaxZ = t1.z + c1.depth / 2;

        float t2MinX = t2.x - c2.width / 2, t2MaxX = t2.x + c2.width / 2;
        float t2MinY = t2.y - c2.height / 2, t2MaxY = t2.y + c2.height / 2;
        float t2MinZ = t2.z - c2.depth / 2, t2MaxZ = t2.z + c2.depth / 2;

        return (t1MinX <= t2MaxX && t1MaxX >= t2MinX) &&
                (t1MinY <= t2MaxY && t1MaxY >= t2MinY) &&
                (t1MinZ <= t2MaxZ && t1MaxZ >= t2MinZ);
    }

    private void checkCollisions() {
        List<Integer> bulletsToRemove = new ArrayList<>();
        List<Integer> entitiesToRemove = new ArrayList<>();

        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> bulletEntry : new ArrayList<>(
                ecs.getEntities().entrySet())) {
            int bulletId = bulletEntry.getKey();

            Map<Class<? extends Component>, Component> bulletComps = bulletEntry.getValue();

            if (!bulletComps.containsKey(BulletComponent.class) || !bulletComps.containsKey(ColliderComponent.class))
                continue;

            TransformComponent bulletTransform = (TransformComponent) bulletComps.get(TransformComponent.class);
            ColliderComponent bulletCollider = (ColliderComponent) bulletComps.get(ColliderComponent.class);

            for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> otherEntry : new ArrayList<>(
                    ecs.getEntities().entrySet())) {
                int otherId = otherEntry.getKey();

                if (otherId == bulletId)
                    continue;

                Map<Class<? extends Component>, Component> otherComps = otherEntry.getValue();

                if (!otherComps.containsKey(ColliderComponent.class))
                    continue;
                if (otherComps.containsKey(BulletComponent.class))
                    continue;

                TransformComponent otherTransform = (TransformComponent) otherComps.get(TransformComponent.class);
                ColliderComponent otherCollider = (ColliderComponent) otherComps.get(ColliderComponent.class);

                if (checkCollision(bulletTransform, bulletCollider, otherTransform, otherCollider)) {
                    bulletsToRemove.add(bulletId);

                    spawnExplosion(bulletTransform.x, bulletTransform.y, bulletTransform.z);

                    HealthComponent health = (HealthComponent) otherComps.get(HealthComponent.class);
                    if (health != null) {
                        health.health--;
                        if (health.health <= 0) {
                            entitiesToRemove.add(otherId);
                        }
                    }
                    break;
                }
            }
        }
        for (int id : bulletsToRemove)
            ecs.removeEntity(id);
        for (int id : entitiesToRemove)
            ecs.removeEntity(id);
    }

    private void updateExplosions(float dt) {
        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            int id = entry.getKey();

            Map<Class<? extends Component>, Component> comps = entry.getValue();

            if (comps.containsKey(ExplosionComponent.class)) {
                ExplosionComponent explosion = (ExplosionComponent) comps.get(ExplosionComponent.class);

                explosion.lifetime -= dt;
                if (explosion.lifetime <= 0) {
                    toRemove.add(id);
                }
            }
        }

        for (int id : toRemove) {
            ecs.removeEntity(id);
        }
    }

    private void spawnExplosion(float x, float y, float z) {
        int explosionEntity = ecs.createEntity();

        TransformComponent transform = new TransformComponent(x, y, z);

        ecs.addComponent(explosionEntity, transform);
        ecs.addComponent(explosionEntity, new MeshComponent(cubeMesh));
        ecs.addComponent(explosionEntity, new ExplosionComponent(1.0f));
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentTime = (float) glfwGetTime();
            float dt = currentTime - lastFrameTime;
            lastFrameTime = currentTime;

            playerController.update(window);
            TransformComponent playerTransform = ecs.getComponent(playerEntity, TransformComponent.class);
            camera.position.set(playerTransform.x, playerTransform.y, playerTransform.z);

            updateBullets(dt);
            checkCollisions();
            updateExplosions(dt);

            renderer.render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                weapon.fire(ecs, bulletMesh, playerTransform.x, playerTransform.y, playerTransform.z,
                        0.0f, 0.0f, -1.0f);
            }
        }
    }

    private void cleanup() {
        if (shader != null)
            shader.cleanup();

        for (Map<Class<? extends Component>, Component> comps : ecs.getEntities().values()) {
            MeshComponent meshComp = (MeshComponent) comps.get(MeshComponent.class);
            if (meshComp != null && meshComp.mesh != null) {
                meshComp.mesh.cleanup();
            }
        }

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
    }

    public void run() {
        try {
            init();
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
            glfwTerminate();
            glfwSetErrorCallback(null).free();
        }
    }

    public static void main(String[] args) {
        new FPSGame().run();
    }
}
