package com.atakmap.android.arglasses.data;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides data from ATAK to the AR glasses service.
 * Collects map data, POIs, and device orientation.
 */
public class ARGlassesDataProvider implements SensorEventListener {

    private static final String TAG = "ARGlassesDataProvider";
    
    private final Context context;
    private MapView mapView;
    
    // Sensor variables for device orientation
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private boolean hasAccelerometerReading = false;
    private boolean hasMagnetometerReading = false;
    
    /**
     * Constructor
     * @param context The application context
     */
    public ARGlassesDataProvider(Context context) {
        this.context = context;
        
        // Get MapView instance
        this.mapView = MapView.getMapView();
        
        // Initialize sensors for device orientation
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        // Register sensor listeners
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, 
                    SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, magnetometer, 
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.w(TAG, "Sensors not available");
        }
    }
    
    /**
     * Get the current device location from ATAK
     * @return The current location as a GeoPoint
     */
    public GeoPoint getSelfLocation() {
        if (mapView != null && mapView.getSelfMarker() != null) {
            return mapView.getSelfMarker().getPoint();
        }
        return null;
    }
    
    /**
     * Get the current zoom level from ATAK
     * @return The current zoom level
     */
    public double getZoomLevel() {
        if (mapView != null) {
            return mapView.getMapScale();
        }
        return 0;
    }
    
    /**
     * Get the current map bearing from ATAK
     * @return The current map bearing in degrees
     */
    public double getMapBearing() {
        if (mapView != null) {
            return mapView.getMapRotation();
        }
        return 0;
    }
    
    /**
     * Get a list of POIs from the map
     * @return List of POI data objects
     */
    public List<POIData> getPointsOfInterest() {
        List<POIData> pois = new ArrayList<>();
        
        if (mapView == null) {
            return pois;
        }
        
        // Get all map items in view
        MapGroup rootGroup = mapView.getRootGroup();
        Collection<MapItem> items = rootGroup.getAllItems();
        
        for (MapItem item : items) {
            // Only include markers with valid points
            if (item instanceof Marker && item.getPoint() != null) {
                Marker marker = (Marker) item;
                
                // Create POI data object
                POIData poi = new POIData(
                        marker.getUID(),
                        marker.getTitle(),
                        determineMarkerType(marker),
                        marker.getPoint(),
                        marker.getStrokeColor()
                );
                
                pois.add(poi);
            }
        }
        
        return pois;
    }
    
    /**
     * Determines the type of marker based on its properties
     * @param marker The marker to check
     * @return The type of the marker as a string
     */
    private String determineMarkerType(Marker marker) {
        // Try to determine marker type based on icon, style, etc.
        String type = marker.getType();
        
        // If no type available, check icon path
        if (type == null || type.isEmpty()) {
            String iconPath = marker.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                // Extract the last part of the icon path as the type
                String[] parts = iconPath.split("/");
                if (parts.length > 0) {
                    return parts[parts.length - 1].replace(".png", "");
                }
            }
            
            // Default type if nothing else available
            return "marker";
        }
        
        return type;
    }
    
    /**
     * Get the device heading (azimuth) in degrees
     * @return The heading in degrees
     */
    public float getDeviceHeading() {
        updateOrientationAngles();
        // Orientation[0] is azimuth (heading), convert from radians to degrees
        return (float) Math.toDegrees(orientationAngles[0]);
    }
    
    /**
     * Get the device tilt (pitch) in degrees
     * @return The tilt in degrees
     */
    public float getDeviceTilt() {
        updateOrientationAngles();
        // Orientation[1] is pitch (tilt), convert from radians to degrees
        return (float) Math.toDegrees(orientationAngles[1]);
    }
    
    /**
     * Get the device roll in degrees
     * @return The roll in degrees
     */
    public float getDeviceRoll() {
        updateOrientationAngles();
        // Orientation[2] is roll, convert from radians to degrees
        return (float) Math.toDegrees(orientationAngles[2]);
    }
    
    /**
     * Update orientation angles from sensor readings
     */
    private void updateOrientationAngles() {
        if (!hasAccelerometerReading || !hasMagnetometerReading) {
            return;
        }
        
        if (SensorManager.getRotationMatrix(rotationMatrix, null, 
                accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 
                    0, accelerometerReading.length);
            hasAccelerometerReading = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 
                    0, magnetometerReading.length);
            hasMagnetometerReading = true;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
    
    /**
     * Clean up resources
     */
    public void dispose() {
        // Unregister sensor listeners
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Clear references
        mapView = null;
    }
}
