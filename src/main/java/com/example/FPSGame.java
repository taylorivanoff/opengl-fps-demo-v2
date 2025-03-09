package com.example;

import java.lang.Math;
import java.nio.*;
import java.util.*;

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

public class FPSGame {

    // ===== ECS Framework =====
    public interface Component {
    }

    // Basic transform component (position and rotation)
    public static class TransformComponent implements Component {
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

    // Holds mesh data for rendering an entity
    public static class MeshComponent implements Component {
        public Mesh mesh;

        public MeshComponent(Mesh mesh) {
            this.mesh = mesh;
        }
    }

    // Component for bullets: holds velocity, acceleration, and lifetime.
    public static class BulletComponent implements Component {
        public Vector3f velocity;
        public Vector3f acceleration;
        public float lifeTime; // seconds

        public BulletComponent(Vector3f velocity, Vector3f acceleration, float lifeTime) {
            this.velocity = velocity;
            this.acceleration = acceleration;
            this.lifeTime = lifeTime;
        }
    }

    // Component for collision detection (AABB)
    public static class ColliderComponent implements Component {
        public float width, height, depth;

        public ColliderComponent(float width, float height, float depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }

    // Component for explosion visual effects.
    public static class ExplosionComponent implements Component {
        public float lifetime; // Remaining lifetime
        public float maxLifetime; // Initial lifetime (for fade calculations)

        public ExplosionComponent(float lifetime) {
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
        }
    }

    // Simple ECS registry that maps entity IDs to component maps.
    public static class ECSRegistry {
        private Map<Integer, Map<Class<? extends Component>, Component>> entities = new HashMap<>();
        private int nextEntityId = 0;

        public int createEntity() {
            int id = nextEntityId++;
            entities.put(id, new HashMap<>());
            return id;
        }

        public <T extends Component> void addComponent(int entity, T component) {
            Map<Class<? extends Component>, Component> comps = entities.get(entity);
            if (comps != null) {
                comps.put(component.getClass(), component);
            }
        }

        public <T extends Component> T getComponent(int entity, Class<T> compClass) {
            Map<Class<? extends Component>, Component> comps = entities.get(entity);
            if (comps != null) {
                return compClass.cast(comps.get(compClass));
            }
            return null;
        }

        public Map<Integer, Map<Class<? extends Component>, Component>> getEntities() {
            return entities;
        }

        public void removeEntity(int entityId) {
            entities.remove(entityId);
        }
    }

    // ===== Weapon System =====
    // The weapon now spawns a bullet entity with a collider and physics.
    public static abstract class Weapon {
        protected float damage;
        protected float bulletSpeed;

        public abstract void fire(ECSRegistry ecs, Mesh bulletMesh,
                float startX, float startY, float startZ,
                float directionX, float directionY, float directionZ);
    }

    public static class Rifle extends Weapon {
        public Rifle() {
            this.damage = 25.0f;
            this.bulletSpeed = 50.0f;
        }

        @Override
        public void fire(ECSRegistry ecs, Mesh bulletMesh,
                float startX, float startY, float startZ,
                float directionX, float directionY, float directionZ) {
            int bulletEntity = ecs.createEntity();
            TransformComponent bulletTransform = new TransformComponent(startX, startY, startZ);
            ecs.addComponent(bulletEntity, bulletTransform);
            ecs.addComponent(bulletEntity, new MeshComponent(bulletMesh));
            // Add a small collider for the bullet
            ecs.addComponent(bulletEntity, new ColliderComponent(0.2f, 0.2f, 0.2f));
            // Compute the bullet's initial velocity (normalized direction scaled by
            // bulletSpeed)
            Vector3f direction = new Vector3f(directionX, directionY, directionZ).normalize();
            Vector3f velocity = new Vector3f(direction).mul(bulletSpeed);
            // Apply gravity as a constant acceleration
            Vector3f acceleration = new Vector3f(0, -9.8f, 0);
            ecs.addComponent(bulletEntity, new BulletComponent(velocity, acceleration, 5.0f));
            System.out.println("Bullet fired visually!");
        }
    }

    // ===== Player Controller =====
    // Handles basic WASD input to move the player.
    public static class PlayerController {
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

    // ===== Shader Program =====
    // Compiles and links vertex/fragment shaders. Now supports an "alpha" uniform.
    public static class ShaderProgram {
        private int programId;

        public ShaderProgram(String vertexSource, String fragmentSource) throws Exception {
            int vertexShaderId = createShader(vertexSource, GL_VERTEX_SHADER);
            int fragmentShaderId = createShader(fragmentSource, GL_FRAGMENT_SHADER);

            programId = glCreateProgram();
            if (programId == 0) {
                throw new Exception("Could not create Shader");
            }
            glAttachShader(programId, vertexShaderId);
            glAttachShader(programId, fragmentShaderId);
            glLinkProgram(programId);

            int status = glGetProgrami(programId, GL_LINK_STATUS);
            if (status == 0) {
                throw new Exception("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
            }

            glValidateProgram(programId);
            status = glGetProgrami(programId, GL_VALIDATE_STATUS);
            if (status == 0) {
                System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
            }

            glDetachShader(programId, vertexShaderId);
            glDetachShader(programId, fragmentShaderId);
            glDeleteShader(vertexShaderId);
            glDeleteShader(fragmentShaderId);
        }

        private int createShader(String source, int shaderType) throws Exception {
            int shaderId = glCreateShader(shaderType);
            if (shaderId == 0) {
                throw new Exception("Error creating shader. Type: " + shaderType);
            }
            glShaderSource(shaderId, source);
            glCompileShader(shaderId);
            int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
            if (status == 0) {
                throw new Exception("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
            }
            return shaderId;
        }

        public void use() {
            glUseProgram(programId);
        }

        public void setUniformMat4(String name, Matrix4f matrix) {
            int location = glGetUniformLocation(programId, name);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                matrix.get(fb);
                glUniformMatrix4fv(location, false, fb);
            }
        }

        public void setUniform1f(String name, float value) {
            int location = glGetUniformLocation(programId, name);
            glUniform1f(location, value);
        }

        public void cleanup() {
            if (programId != 0) {
                glDeleteProgram(programId);
            }
        }
    }

    // ===== Camera =====
    // Uses JOML to generate view and projection matrices.
    public static class Camera {
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

    // ===== Mesh =====
    // Wraps a VAO/VBO/EBO setup for a cube mesh.
    public static class Mesh {
        private int vaoId;
        private int vboId;
        private int eboId;
        private int vertexCount;

        public Mesh(float[] vertices, int[] indices) {
            vertexCount = indices.length;
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
            vertexBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
            MemoryUtil.memFree(vertexBuffer);

            eboId = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
            MemoryUtil.memFree(indicesBuffer);

            // Attribute pointers: position (location 0) and color (location 1)
            int stride = (3 + 3) * Float.BYTES;
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        }

        public void draw() {
            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }

        public void cleanup() {
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
        }
    }

    // ===== Renderer =====
    // Renders entities by iterating over all entities that have Transform and Mesh
    // components.
    // For explosion entities, it scales the model and sets alpha based on remaining
    // lifetime.
    public static class Renderer {
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

            for (Map<Class<? extends Component>, Component> comps : ecs.getEntities().values()) {
                TransformComponent transform = (TransformComponent) comps.get(TransformComponent.class);
                MeshComponent meshComp = (MeshComponent) comps.get(MeshComponent.class);
                if (transform != null && meshComp != null) {
                    Matrix4f model = new Matrix4f()
                            .translation(transform.x, transform.y, transform.z)
                            .rotateXYZ(transform.rotationX, transform.rotationY, transform.rotationZ);

                    float alpha = 1.0f;
                    // If the entity is an explosion, scale it up and fade it out.
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

    // ===== Main Game Fields and Loop =====
    private long window;
    private ECSRegistry ecs;
    private PlayerController playerController;
    private Renderer renderer;
    private Weapon weapon;
    private Camera camera;
    private ShaderProgram shader;
    private Mesh bulletMesh; // Shared bullet mesh
    private Mesh cubeMesh; // Used for scene objects and explosion effects
    private int playerEntity;
    private float lastFrameTime;

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

    private void init() throws Exception {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        window = glfwCreateWindow(800, 600, "Java FPS", NULL, NULL);
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
        playerController = new PlayerController(playerEntity, ecs);

        // Create a cube entity as a static scene object
        int cubeEntity = ecs.createEntity();
        ecs.addComponent(cubeEntity, new TransformComponent(0.0f, 0.0f, -5.0f));
        cubeMesh = createCubeMesh();
        ecs.addComponent(cubeEntity, new MeshComponent(cubeMesh));
        ecs.addComponent(cubeEntity, new ColliderComponent(1.0f, 1.0f, 1.0f));

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

        // Initialize camera
        camera = new Camera(45.0f, 800f / 600f, 0.1f, 100f);
        TransformComponent playerTransform = ecs.getComponent(playerEntity, TransformComponent.class);
        camera.position.set(playerTransform.x, playerTransform.y, playerTransform.z);
        camera.target.set(0, 0, -1);

        renderer = new Renderer(shader, camera, ecs);
        weapon = new Rifle();

        lastFrameTime = (float) glfwGetTime();
    }

    // Creates a simple cube mesh with positions and colors.
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

    // ===== Physics and Collision Systems =====

    // Update bullet physics: integrate velocity with acceleration and update
    // position.
    private void updateBullets(float dt) {
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            int id = entry.getKey();
            Map<Class<? extends Component>, Component> comps = entry.getValue();
            if (comps.containsKey(BulletComponent.class) && comps.containsKey(TransformComponent.class)) {
                BulletComponent bullet = (BulletComponent) comps.get(BulletComponent.class);
                TransformComponent transform = (TransformComponent) comps.get(TransformComponent.class);
                // Update velocity with acceleration (e.g. gravity)
                bullet.velocity.add(new Vector3f(bullet.acceleration).mul(dt));
                // Update position based on current velocity
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

    // A simple AABB collision test between two entities.
    private boolean checkCollision(TransformComponent t1, ColliderComponent c1,
            TransformComponent t2, ColliderComponent c2) {
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

    // Check collisions between bullet entities and other collidable objects.
    private void checkCollisions() {
        List<Integer> bulletsToRemove = new ArrayList<>();
        // Iterate over bullet entities.
        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> bulletEntry : ecs.getEntities()
                .entrySet()) {
            int bulletId = bulletEntry.getKey();
            Map<Class<? extends Component>, Component> bulletComps = bulletEntry.getValue();
            if (!bulletComps.containsKey(BulletComponent.class) || !bulletComps.containsKey(ColliderComponent.class))
                continue;
            TransformComponent bulletTransform = (TransformComponent) bulletComps.get(TransformComponent.class);
            ColliderComponent bulletCollider = (ColliderComponent) bulletComps.get(ColliderComponent.class);

            // Check against all other collidable entities (skip bullets)
            for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> otherEntry : ecs.getEntities()
                    .entrySet()) {
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
                    // On collision, mark bullet for removal and spawn an explosion.
                    bulletsToRemove.add(bulletId);
                    spawnExplosion(bulletTransform.x, bulletTransform.y, bulletTransform.z);
                    break;
                }
            }
        }
        for (int id : bulletsToRemove) {
            ecs.removeEntity(id);
        }
    }

    // Update explosion effects: reduce lifetime and remove expired explosions.
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

    // Spawns an explosion effect entity at the given location.
    private void spawnExplosion(float x, float y, float z) {
        int explosionEntity = ecs.createEntity();
        TransformComponent transform = new TransformComponent(x, y, z);
        ecs.addComponent(explosionEntity, transform);
        // Reuse the cube mesh for the explosion effect.
        ecs.addComponent(explosionEntity, new MeshComponent(cubeMesh));
        ecs.addComponent(explosionEntity, new ExplosionComponent(1.0f)); // lasts 1 second
        System.out.println("Explosion spawned at (" + x + ", " + y + ", " + z + ")");
    }

    // ===== Main Loop =====
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentTime = (float) glfwGetTime();
            float dt = currentTime - lastFrameTime;
            lastFrameTime = currentTime;

            // Update player input/movement.
            playerController.update(window);
            TransformComponent playerTransform = ecs.getComponent(playerEntity, TransformComponent.class);
            camera.position.set(playerTransform.x, playerTransform.y, playerTransform.z);

            // Update bullet physics, collision detection, and explosion effects.
            updateBullets(dt);
            checkCollisions();
            updateExplosions(dt);

            renderer.render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            // Define the cooldown duration (e.g., 0.5 seconds)
            float FIRE_COOLDOWN = 0.5f;

            // Variable to store the time of the last shot
            float lastShotTime = -FIRE_COOLDOWN; // Initialize to allow firing immediately

            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                // Check if enough time has passed since the last shot
                if (currentTime - lastShotTime >= FIRE_COOLDOWN) {
                    // Fire the bullet
                    weapon.fire(ecs, bulletMesh, playerTransform.x, playerTransform.y, playerTransform.z,
                            0.0f, 0.0f, -1.0f);

                    // Update the last shot time
                    lastShotTime = currentTime;
                }
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

    public static void main(String[] args) {
        new FPSGame().run();
    }
}
