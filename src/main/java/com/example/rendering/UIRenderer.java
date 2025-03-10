package com.example.rendering;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import com.example.components.*;

public class UIRenderer {
    private ShaderProgram uiShader;

    public UIRenderer() throws Exception {
        // UI vertex shader (in NDC)
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec2 aPos;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                "}\n";
        // UI fragment shader (solid white)
        String fragmentSource = "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "}\n";
        uiShader = new ShaderProgram(vertexSource, fragmentSource);
    }

    // Render all UI entities provided by the ECS
    public void render(Iterable<Map<Class<? extends Component>, Component>> uiEntities) {
        // Disable depth testing so UI appears on top
        glDisable(GL_DEPTH_TEST);
        uiShader.use();

        for (Map<Class<? extends Component>, Component> comps : uiEntities) {
            UIComponent uiComp = (UIComponent) comps.get(UIComponent.class);
            if (uiComp != null) {
                glBindVertexArray(uiComp.vaoId);
                // Draw using GL_LINES if your crosshair is defined as lines
                glDrawArrays(GL_LINES, 0, uiComp.vertexCount);
            }
        }
        glBindVertexArray(0);
        glEnable(GL_DEPTH_TEST);
    }

    public void cleanup() {
        uiShader.cleanup();
    }
}
