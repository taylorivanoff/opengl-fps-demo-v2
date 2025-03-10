package com.example.components;

import org.joml.*;

public class AIComponent implements Component {
    public enum AIState {
        PATROL,
        DETECT,
        COMBAT
    }

    public AIState currentState = AIState.PATROL;
    public Vector3f[] waypoints;
    public int currentWaypoint = 0;
    public float detectionRange = 10.0f;
    public float fieldOfView = 60.0f; // Degrees
    public float combatRange = 5.0f;
    public float patrolSpeed = 2.0f;
    public float combatSpeed = 4.0f;
    public float detectionTime = 1.0f;
    public float timeSinceDetection = 0.0f;
    public int targetPlayerEntity = -1;
}
