package com.example.ta_mask_detection.common.rendering;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.content.ContentValues.TAG;

public class BackgroundRenderer {

    private static final String CAMERA_VERTEX_SHADER_NAME = "shaders/screenquad.vert";
    private static final String CAMERA_FRAGMENT_SHADER_NAME = "shaders/screenquad.frag";
    private static final String DEPTH_VISUALIZER_VERTEX_SHADER_NAME = "shaders/background_show_depth_color_visualization.vert";
    private static final String DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME = "shaders/background_show_depth_color_visualization.frag";

    private static final int COORDS_PER_VERTEX = 2;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;

    private FloatBuffer quadCoords;
    private FloatBuffer quadTexCoords;

    private int cameraProgram;
    private int depthProgram;

    private int cameraPositionAttrib;
    private int cameraTexCoordAttrib;
    private int cameraTextureUniform;
    private int cameraTextureId = -1;
    private boolean suppressTimestampZeroRendering = true;

    private int depthPositionAttrib;
    private int depthTexCoordAttrib;
    private int depthTextureUniform;
    private int depthTextureId = -1;

    public int getTextureId()
    {
        return cameraTextureId;
    }

    private static final float[] QUAD_COORDS= new float[] {
      -1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f,
    };

    public void createOnGlThread(Context context, int depthTextureId) throws IOException{
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        cameraTextureId = textures[0];
        int textureTarget = GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES;
        GLES20.glBindTexture(textureTarget, cameraTextureId);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        int numVertices = 4;
        if(numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX){
            throw new RuntimeException("unexpected number of vertices in background");
        }

        ByteBuffer bbCoord = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbCoord.order(ByteOrder.nativeOrder());
        quadCoords = bbCoord.asFloatBuffer();
        quadCoords.put(QUAD_COORDS);
        quadCoords.position(0);

        ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        quadTexCoords = bbTexCoordsTransformed.asFloatBuffer();

        //load camera render
        {
            int vertexShader =
                    ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, CAMERA_VERTEX_SHADER_NAME);
            int fragmentShader =
                    ShaderUtil.loadGLShader(
                            TAG, context, GLES20.GL_FRAGMENT_SHADER, CAMERA_FRAGMENT_SHADER_NAME);

            cameraProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(cameraProgram, vertexShader);
            GLES20.glAttachShader(cameraProgram, fragmentShader);
            GLES20.glLinkProgram(cameraProgram);
            GLES20.glUseProgram(cameraProgram);
            cameraPositionAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_Position");
            cameraTexCoordAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord");
            ShaderUtil.checkGLError(TAG, "Program creation");

            cameraTextureUniform = GLES20.glGetUniformLocation(cameraProgram, "sTexture");
            ShaderUtil.checkGLError(TAG, "Program parameters");
        }
        // Load render depth map shader.
        {
            int vertexShader =
                    ShaderUtil.loadGLShader(
                            TAG, context, GLES20.GL_VERTEX_SHADER, DEPTH_VISUALIZER_VERTEX_SHADER_NAME);
            int fragmentShader =
                    ShaderUtil.loadGLShader(
                            TAG, context, GLES20.GL_FRAGMENT_SHADER, DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME);

            depthProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(depthProgram, vertexShader);
            GLES20.glAttachShader(depthProgram, fragmentShader);
            GLES20.glLinkProgram(depthProgram);
            GLES20.glUseProgram(depthProgram);
            depthPositionAttrib = GLES20.glGetAttribLocation(depthProgram, "a_Position");
            depthTexCoordAttrib = GLES20.glGetAttribLocation(depthProgram, "a_TexCoord");
            ShaderUtil.checkGLError(TAG, "Program creation");

            depthTextureUniform = GLES20.glGetUniformLocation(depthProgram, "u_DepthTexture");
            ShaderUtil.checkGLError(TAG, "Program parameters");
        }
        this.depthTextureId = depthTextureId;
    }

    public void createOnGlThread(Context context) throws IOException {
        createOnGlThread(context, /*depthTextureId=*/ -1);
    }

    public void suppressTimestampZeroRendering(boolean suppressTimestampZeroRendering) {
        this.suppressTimestampZeroRendering = suppressTimestampZeroRendering;
    }

    public void draw(@NonNull Frame frame, boolean debugShowDepthMap) {
        // If display rotation changed (also includes view size change), we need to re-query the uv
        // coordinates for the screen rect, as they may have changed as well.
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadCoords, Coordinates2d.TEXTURE_NORMALIZED, quadTexCoords);
        }

        if (frame.getTimestamp() == 0 && suppressTimestampZeroRendering) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            return;
        }

        draw(debugShowDepthMap);
    }

    public void draw(@NonNull Frame frame) {
        draw(frame, /*debugShowDepthMap=*/ false);
    }

    public void draw(
            int imageWidth, int imageHeight, float screenAspectRatio, int cameraToDisplayRotation) {
        // Crop the camera image to fit the screen aspect ratio.
        float imageAspectRatio = (float) imageWidth / imageHeight;
        float croppedWidth;
        float croppedHeight;
        if (screenAspectRatio < imageAspectRatio) {
            croppedWidth = imageHeight * screenAspectRatio;
            croppedHeight = imageHeight;
        } else {
            croppedWidth = imageWidth;
            croppedHeight = imageWidth / screenAspectRatio;
        }

        float u = (imageWidth - croppedWidth) / imageWidth * 0.5f;
        float v = (imageHeight - croppedHeight) / imageHeight * 0.5f;

        float[] texCoordTransformed;
        switch (cameraToDisplayRotation) {
            case 90:
                texCoordTransformed = new float[] {1 - u, 1 - v, 1 - u, v, u, 1 - v, u, v};
                break;
            case 180:
                texCoordTransformed = new float[] {1 - u, v, u, v, 1 - u, 1 - v, u, 1 - v};
                break;
            case 270:
                texCoordTransformed = new float[] {u, v, u, 1 - v, 1 - u, v, 1 - u, 1 - v};
                break;
            case 0:
                texCoordTransformed = new float[] {u, 1 - v, 1 - u, 1 - v, u, v, 1 - u, v};
                break;
            default:
                throw new IllegalArgumentException("Unhandled rotation: " + cameraToDisplayRotation);
        }

        // Write image texture coordinates.
        quadTexCoords.position(0);
        quadTexCoords.put(texCoordTransformed);

        draw(/*debugShowDepthMap=*/ false);
    }

    private void draw(boolean debugShowDepthMap) {
        // Ensure position is rewound before use.
        quadTexCoords.position(0);

        // No need to test or write depth, the screen quad has arbitrary depth, and is expected
        // to be drawn first.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        if (debugShowDepthMap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId);
            GLES20.glUseProgram(depthProgram);
            GLES20.glUniform1i(depthTextureUniform, 0);

            // Set the vertex positions and texture coordinates.
            GLES20.glVertexAttribPointer(
                    depthPositionAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords);
            GLES20.glVertexAttribPointer(
                    depthTexCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords);
            GLES20.glEnableVertexAttribArray(depthPositionAttrib);
            GLES20.glEnableVertexAttribArray(depthTexCoordAttrib);
        } else {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
            GLES20.glUseProgram(cameraProgram);
            GLES20.glUniform1i(cameraTextureUniform, 0);

            // Set the vertex positions and texture coordinates.
            GLES20.glVertexAttribPointer(
                    cameraPositionAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords);
            GLES20.glVertexAttribPointer(
                    cameraTexCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords);
            GLES20.glEnableVertexAttribArray(cameraPositionAttrib);
            GLES20.glEnableVertexAttribArray(cameraTexCoordAttrib);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        if (debugShowDepthMap) {
            GLES20.glDisableVertexAttribArray(depthPositionAttrib);
            GLES20.glDisableVertexAttribArray(depthTexCoordAttrib);
        } else {
            GLES20.glDisableVertexAttribArray(cameraPositionAttrib);
            GLES20.glDisableVertexAttribArray(cameraTexCoordAttrib);
        }

        // Restore the depth state for further drawing.
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        ShaderUtil.checkGLError(TAG, "BackgroundRendererDraw");
    }

}
