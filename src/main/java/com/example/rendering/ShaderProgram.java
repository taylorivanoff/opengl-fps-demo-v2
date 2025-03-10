package com.example.rendering;

import java.nio.*;

import org.joml.*;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.*;

public class ShaderProgram {
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