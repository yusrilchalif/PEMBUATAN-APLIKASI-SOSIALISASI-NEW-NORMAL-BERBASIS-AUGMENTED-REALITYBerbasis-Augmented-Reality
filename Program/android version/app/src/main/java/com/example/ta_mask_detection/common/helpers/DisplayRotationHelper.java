package com.example.ta_mask_detection.common.helpers;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.google.ar.core.Session;

public class DisplayRotationHelper implements DisplayManager.DisplayListener {
    private boolean viewportChanged;
    private int viewportWidth;
    private int viewportHeight;
    private final Display display;
    private final DisplayManager displayManager;
    private final CameraManager cameraManager;

    public DisplayRotationHelper(Context context){
        displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public void onResume(){
        displayManager.registerDisplayListener(this, null);
    }

    public void onPause(){
        displayManager.unregisterDisplayListener(this);
    }

    public void onSurfaceChange(int width, int height){
        viewportWidth = width;
        viewportHeight = height;
        viewportChanged = true;
    }

    public void updateSessionIfNeeded(Session session){
        if(viewportChanged){
            int displayRotation = display.getRotation();
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
            viewportChanged = false;
        }
    }

    public float getCameraSensorRelativeViewportAspectRatio(String cameraId) {
        float aspectRatio;
        int cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId);
        switch (cameraSensorToDisplayRotation) {
            case 90:
            case 270:
                aspectRatio = (float) viewportHeight / (float) viewportWidth;
                break;
            case 0:
            case 180:
                aspectRatio = (float) viewportWidth / (float) viewportHeight;
                break;
            default:
                throw new RuntimeException("Unhandled rotation: " + cameraSensorToDisplayRotation);
        }
        return aspectRatio;
    }

    public int getCameraSensorToDisplayRotation(String cameraId) {
        CameraCharacteristics characteristics;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Unable to determine display orientation", e);
        }

        // Camera sensor orientation.
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Current display orientation.
        int displayOrientation = toDegrees(display.getRotation());

        // Make sure we return 0, 90, 180, or 270 degrees.
        return (sensorOrientation - displayOrientation + 360) % 360;
    }

    private int toDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                throw new RuntimeException("Unknown rotation " + rotation);
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {}

    @Override
    public void onDisplayChanged(int displayId) {
        viewportChanged = true;
    }
}

