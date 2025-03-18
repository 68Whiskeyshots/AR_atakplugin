// app/src/main/java/com/atakmap/android/apex/CompassManager.java
package com.atakmap.android.apex;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.OnPanListener;
import com.atakmap.android.sensors.SensorMapListener;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Manages compass heading data for sending to DigiLens
 */
public class CompassManager implements OnPanListener {
    private static final String TAG = "CompassManager";
    
    private final MapView mapView;
    private final DigiLensConnectionManager digiLensManager;
    private final Handler handler;
    
    private boolean isRunning = false;
    private float currentHeading = 0f;
    private int updateIntervalMs = 250; // 4 updates per second
    
    /**
     * Constructor
     */
    public CompassManager(MapView mapView, DigiLensConnectionManager digiLensManager) {
        this.mapView = mapView;
        this.digiLensManager = digiLensManager;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start the compass manager
     */
    public void start() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // Register for map rotation updates
        mapView.addOnPanListener(this);
        
        // Schedule regular heading updates
        handler.post(updateHeadingRunnable);
        
        Log.d(TAG, "Compass manager started");
    }
    
    /**
     * Stop the compass manager
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        // Unregister listener
        mapView.removeOnPanListener(this);
        
        // Cancel scheduled updates
        handler.removeCallbacks(updateHeadingRunnable);
        
        Log.d(TAG, "Compass manager stopped");
    }
    
    /**
     * Set update interval
     */
    public void setUpdateIntervalMs(int updateIntervalMs) {
        this.updateIntervalMs = updateIntervalMs;
    }
    
    /**
     * Runnable for updating heading
     */
    private final Runnable updateHeadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) {
                return;
            }
            
            // Get current heading
            float heading = mapView.getMapRotation();
            
            // Only send if changed
            if (Math.abs(heading - currentHeading) > 0.5f) {
                currentHeading = heading;
                digiLensManager.sendCompassData(heading);
            }
            
            // Schedule next update
            handler.postDelayed(this, updateIntervalMs);
        }
    };
    
    @Override
    public void onPan(GeoPoint point, double rotation) {
        // Map rotation changed, update heading immediately
        if (isRunning && Math.abs(rotation - currentHeading) > 0.5f) {
            currentHeading = (float) rotation;
            digiLensManager.sendCompassData(currentHeading);
        }
    }
}
