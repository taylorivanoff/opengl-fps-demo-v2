package com.example.systems;

import java.util.*;

import javax.vecmath.*;

import com.example.components.AIComponent.AIState;
import com.example.components.*;
import com.example.input.*;
import com.example.physics.*;

public class AISystem {
    private final ECSRegistry ecs;
    private final PhysicsWorld physicsWorld;
    private int playerEntity;

    public AISystem(ECSRegistry ecs, PhysicsWorld physicsWorld) {
        this.ecs = ecs;
        this.physicsWorld = physicsWorld;
        findPlayerEntity();
    }

    private void findPlayerEntity() {
        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            if (entry.getValue().containsKey(PlayerController.class)) {
                playerEntity = entry.getKey();
                break;
            }
        }
    }

    public void update(float deltaTime) {
        TransformComponent playerTransform = ecs.getComponent(playerEntity, TransformComponent.class);

        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : ecs.getEntities().entrySet()) {
            int entity = entry.getKey();
            AIComponent ai = ecs.getComponent(entity, AIComponent.class);
            if (ai == null)
                continue;

            TransformComponent transform = ecs.getComponent(entity, TransformComponent.class);
            HealthComponent health = ecs.getComponent(entity, HealthComponent.class);

            if (health != null && health.health <= 0)
                continue;

            // Update AI state
            switch (ai.currentState) {
                case PATROL -> {
                    handlePatrolState(entity, ai, transform, deltaTime);
                    checkPlayerDetection(entity, ai, transform, playerTransform);
                }
                case DETECT -> {
                    System.out.println(entity + ": Detected");

                    handleDetectionState(ai, deltaTime);
                    checkPlayerDetection(entity, ai, transform, playerTransform);
                }
                case COMBAT -> {
                    System.out.println(entity + ": Fighting");

                    handleCombatState(entity, ai, transform, playerTransform, deltaTime);
                }
            }
        }
    }

    private void handlePatrolState(int entity, AIComponent ai, TransformComponent transform, float deltaTime) {
        if (ai.waypoints == null || ai.waypoints.length == 0)
            return;

        org.joml.Vector3f target = ai.waypoints[ai.currentWaypoint];
        Vector3f direction = new Vector3f(target.x - transform.x, target.y - transform.y, target.z - transform.z);

        if (direction.length() < 0.5f) {
            ai.currentWaypoint = (ai.currentWaypoint + 1) % ai.waypoints.length;
            target = ai.waypoints[ai.currentWaypoint];
            direction.set(target.x - transform.x, target.y - transform.y, target.z - transform.z);
        }

        direction.normalize();
        transform.x += direction.x * ai.patrolSpeed * deltaTime;
        transform.y += direction.y * ai.patrolSpeed * deltaTime;
        transform.z += direction.z * ai.patrolSpeed * deltaTime;

        // Update rotation to face movement direction
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        transform.rotationY = yaw;
    }

    private void checkPlayerDetection(int entity, AIComponent ai, TransformComponent transform,
            TransformComponent playerTransform) {
        // Calculate distance to player
        Vector3f toPlayer = new Vector3f(
                playerTransform.x - transform.x,
                playerTransform.y - transform.y,
                playerTransform.z - transform.z);
        float distance = toPlayer.length();

        if (distance > ai.detectionRange)
            return;

        // Calculate angle to player
        Vector3f forward = new Vector3f(
                (float) Math.sin(Math.toRadians(transform.rotationY)),
                0,
                (float) Math.cos(Math.toRadians(transform.rotationY)));
        toPlayer.normalize();
        float dot = toPlayer.dot(forward);
        float angle = (float) Math.toDegrees(Math.acos(dot));

        if (angle <= ai.fieldOfView / 2) {
            if (physicsWorld.raycast(
                    new Vector3f(transform.x, transform.y + 1f, transform.z),
                    new Vector3f(playerTransform.x, playerTransform.y + 1f, playerTransform.z))) {
                ai.targetPlayerEntity = playerEntity;
                ai.currentState = AIState.DETECT;
                ai.timeSinceDetection = 0;
            }
        }
    }

    private void handleDetectionState(AIComponent ai, float deltaTime) {
        ai.timeSinceDetection += deltaTime;
        if (ai.timeSinceDetection >= ai.detectionTime) {
            ai.currentState = AIState.COMBAT;
        }
    }

    private void handleCombatState(int entity, AIComponent ai, TransformComponent transform,
            TransformComponent playerTransform, float deltaTime) {
        // Move towards player
        Vector3f toPlayer = new Vector3f(
                playerTransform.x - transform.x,
                playerTransform.y - transform.y,
                playerTransform.z - transform.z);

        float distance = toPlayer.length();
        if (distance > ai.combatRange) {
            toPlayer.normalize();
            transform.x += toPlayer.x * ai.combatSpeed * deltaTime;
            transform.z += toPlayer.z * ai.combatSpeed * deltaTime;
        }

        // Face player
        float yaw = (float) Math.toDegrees(Math.atan2(toPlayer.x, toPlayer.z));
        transform.rotationY = yaw;

        // Attack logic
        // Weapon enemyWeapon = ecs.getComponent(entity, Weapon.class);
        // if (enemyWeapon != null) {
        // Vector3f fireDirection = new Vector3f(toPlayer).normalize();
        // enemyWeapon.fire(ecs, physicsWorld, bulletMesh,
        // transform.x, transform.y + 1f, transform.z,
        // fireDirection.x, fireDirection.y, fireDirection.z);
        // }
    }
}