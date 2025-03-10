package com.example;

import java.nio.*;
import java.util.*;

import org.joml.Math;
import org.joml.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.linearmath.*;
import com.example.components.*;
import com.example.entities.*;
import com.example.input.*;
import com.example.physics.*;
import com.example.rendering.*;
import com.example.systems.*;

public class FPSGame {
    private long window;
    private ECSRegistry ecs;
    private PhysicsWorld physicsWorld;
    private AISystem aiSystem;
    private PlayerController playerController;
    private UIRenderer uiRenderer;
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
        physicsWorld = new PhysicsWorld();
        cubeMesh = createCubeMesh();
        Mesh cylinderMesh = createCylinderMesh(32, 2f, 1f);

        // Create enemy entity
        int enemyEntity = ecs.createEntity();
        ecs.addComponent(enemyEntity, new TransformComponent(5.0f, 0.0f, -5.0f));
        ecs.addComponent(enemyEntity, new MeshComponent(cylinderMesh));
        ecs.addComponent(enemyEntity, new ColliderComponent(2.0f, 2.0f, 2.0f));
        ecs.addComponent(enemyEntity, new HealthComponent(100));
        // ecs.addComponent(enemyEntity, new Weapon.Pistol());
        AIComponent ai = new AIComponent();
        ai.waypoints = new Vector3f[] {
                new Vector3f(5f, 0f, -5f),
                new Vector3f(-5f, 0f, -5f)
        };
        ecs.addComponent(enemyEntity, ai);

        // Initialize AI System
        aiSystem = new AISystem(ecs, physicsWorld);
        // Create player entity (used for input and camera)
        playerEntity = ecs.createEntity();
        ecs.addComponent(playerEntity, new TransformComponent(0.0f, 0.0f, 3.0f));
        ecs.addComponent(playerEntity, new ColliderComponent(1.0f, 1.0f, 1.0f));
        ecs.addComponent(playerEntity, new HealthComponent(100));
        playerController = new PlayerController(playerEntity, ecs);

        // Create a cube entity as a static scene object
        // Create a box shape for a wall (dimensions are half-extents)
        BoxShape wallShape = new BoxShape(new javax.vecmath.Vector3f(5f, 2.5f, 0.5f));

        // Set up the transform for the wall's position
        Transform wallTransform = new Transform();
        wallTransform.setIdentity();
        wallTransform.origin.set(0f, 0f, -10f);

        // Static objects use zero mass
        float mass = 0f;
        Vector3f inertia = new Vector3f(0, 0, 0);
        DefaultMotionState wallMotionState = new DefaultMotionState(wallTransform);
        RigidBodyConstructionInfo wallRbInfo = new RigidBodyConstructionInfo(mass, wallMotionState, wallShape);
        RigidBody wallBody = new RigidBody(wallRbInfo);

        // Add the wall body to the physics world
        physicsWorld.addRigidBody(wallBody);

        int cubeEntity = ecs.createEntity();
        ecs.addComponent(cubeEntity, new TransformComponent(0.0f, 0.0f, -5.0f));
        ecs.addComponent(cubeEntity, new MeshComponent(cubeMesh));
        ecs.addComponent(cubeEntity, new ColliderComponent(1.0f, 1.0f, 1.0f));
        ecs.addComponent(cubeEntity, new HealthComponent(100));
        ecs.addComponent(cubeEntity, new PhysicsComponent(wallBody));

        // Create a shared bullet mesh.
        bulletMesh = createCubeMesh();

        // Define the crosshair vertices (two lines crossing at the center)
        float[] crosshairVertices = {
                -0.02f, 0.0f, // Left end of horizontal line
                0.02f, 0.0f, // Right end of horizontal line
                0.0f, -0.02f, // Bottom end of vertical line
                0.0f, 0.02f // Top end of vertical line
        };

        // Create VAO and VBO for the crosshair
        int uiVAO = glGenVertexArrays();
        int uiVBO = glGenBuffers();
        glBindVertexArray(uiVAO);
        glBindBuffer(GL_ARRAY_BUFFER, uiVBO);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(crosshairVertices.length);
        buffer.put(crosshairVertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Create ECS entity and attach the UIComponent.
        int crosshairEntity = ecs.createEntity();
        ecs.addComponent(crosshairEntity, new UIComponent(uiVAO, 4));

        // Shader sources (vertex shader passes position and color; fragment shader
        // usesa uniform "alpha")
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
        uiRenderer = new UIRenderer();
        weapon = new Rifle();

        lastFrameTime = (float) glfwGetTime();
    }

    private Mesh createCylinderMesh(int segments, float height, float radius) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Bottom center vertex
        vertices.add(0f);
        vertices.add(-height / 2);
        vertices.add(0f); // Position
        vertices.add(1f);
        vertices.add(1f);
        vertices.add(1f); // Color

        // Bottom circle
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            vertices.add(x);
            vertices.add(-height / 2);
            vertices.add(z); // Position
            vertices.add(1f);
            vertices.add(0f);
            vertices.add(0f); // Color
        }

        // Top center vertex
        vertices.add(0f);
        vertices.add(height / 2);
        vertices.add(0f); // Position
        vertices.add(1f);
        vertices.add(1f);
        vertices.add(1f); // Color

        // Top circle
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            vertices.add(x);
            vertices.add(height / 2);
            vertices.add(z); // Position
            vertices.add(0f);
            vertices.add(0f);
            vertices.add(1f); // Color
        }

        // Bottom face indices
        for (int i = 1; i <= segments; i++) {
            indices.add(0);
            indices.add(i);
            indices.add(i % segments + 1);
        }

        // Top face indices
        int topCenter = segments + 1;
        for (int i = 1; i <= segments; i++) {
            indices.add(topCenter);
            indices.add(topCenter + i);
            indices.add(topCenter + (i % segments) + 1);
        }

        // Side faces
        for (int i = 1; i <= segments; i++) {
            int next = (i % segments) + 1;
            int bottom = i;
            int top = i + segments + 1;
            int topNext = next + segments + 1;

            indices.add(bottom);
            indices.add(next);
            indices.add(top);

            indices.add(next);
            indices.add(topNext);
            indices.add(top);
        }

        // Convert lists to arrays
        float[] verticesArray = new float[vertices.size()];
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        return new Mesh(verticesArray, indicesArray);
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
                    System.out.println("Collided");
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
        for (int entityId : bulletsToRemove) {
            PhysicsComponent physComp = ecs.getComponent(entityId, PhysicsComponent.class);
            if (physComp != null) {
                physicsWorld.removeRigidBody(physComp.rigidBody);
            }
            ecs.removeEntity(entityId);
        }

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

        for (int entityId : toRemove) {
            PhysicsComponent physComp = ecs.getComponent(entityId, PhysicsComponent.class);
            if (physComp != null) {
                physicsWorld.removeRigidBody(physComp.rigidBody);
            }
            ecs.removeEntity(entityId);
        }
    }

    private void updatePhysics(float dt) {
        physicsWorld.stepSimulation(dt);

        // Update ECS TransformComponent from the physics simulation:
        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            Map<Class<? extends Component>, Component> comps = entry.getValue();
            PhysicsComponent physComp = (PhysicsComponent) comps.get(PhysicsComponent.class);
            TransformComponent transform = (TransformComponent) comps.get(TransformComponent.class);
            if (physComp != null && transform != null) {
                Transform trans = new Transform();
                physComp.rigidBody.getMotionState().getWorldTransform(trans);
                javax.vecmath.Vector3f pos = trans.origin;
                transform.x = pos.x;
                transform.y = pos.y;
                transform.z = pos.z;
            }
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

            checkCollisions();
            updatePhysics(dt);
            aiSystem.update(dt);

            updateBullets(dt);
            updateExplosions(dt);

            renderer.render();

            List<Map<Class<? extends Component>, Component>> uiEntities = new ArrayList<>();
            for (Map<Class<? extends Component>, Component> comps : ecs.getEntities().values()) {
                if (comps.containsKey(UIComponent.class)) {
                    uiEntities.add(comps);
                }
            }
            uiRenderer.render(uiEntities);

            glfwSwapBuffers(window);
            glfwPollEvents();

            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                weapon.fire(ecs, physicsWorld, bulletMesh, playerTransform.x, playerTransform.y, playerTransform.z,
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
