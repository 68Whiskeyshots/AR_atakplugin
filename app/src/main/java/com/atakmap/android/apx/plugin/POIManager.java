// app/src/main/java/com/atakmap/android/apex/POIManager.java
package com.atakmap.android.apex;

import android.os.Handler;
import android.os.Looper;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.UUID;

/**
 * Manages Points of Interest from ATAK for sending to DigiLens
 */
public class POIManager implements MapEventDispatcher.MapEventDispatchListener {
    private static final String TAG = "POIManager";
    
    private final MapView mapView;
    private final DigiLensConnectionManager digiLensManager;
    private final Handler handler;
    
    private boolean isRunning = false;
    private double maxDistanceMeters = 1000.0; // 1 km default
    private int updateIntervalMs = 2000; // 2 seconds
    
    /**
     * Constructor
     */
    public POIManager(MapView mapView, DigiLensConnectionManager digiLensManager) {
        this.mapView = mapView;
        this.digiLensManager = digiLensManager;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start the POI manager
     */
    public void start() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // Register for map events
        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED, this);
        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_REMOVED, this);
        
        // Schedule regular POI updates
        handler.post(updatePOIsRunnable);
        
        Log.d(TAG, "POI manager started");
    }
    
    /**
     * Stop the POI manager
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        // Unregister map event listeners
        mapView.getMapEventDispatcher().removeMapEventListener(MapEvent.ITEM_ADDED, this);
        mapView.getMapEventDispatcher().removeMapEventListener(MapEvent.ITEM_REMOVED, this);
        
        // Cancel scheduled updates
        handler.removeCallbacks(updatePOIsRunnable);
        
        Log.d(TAG, "POI manager stopped");
    }
    
    /**
     * Set maximum distance for POIs
     */
    public void setMaxDistanceMeters(double maxDistanceMeters) {
        this.maxDistanceMeters = maxDistanceMeters;
    }
    
    /**
     * Set update interval
     */
    public void setUpdateIntervalMs(int updateIntervalMs) {
        this.updateIntervalMs = updateIntervalMs;
    }
    
    /**
     * Runnable for updating POIs
     */
    private final Runnable updatePOIsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) {
                return;
            }
            
            updateVisiblePOIs();
            
            // Schedule next update
            handler.postDelayed(this, updateIntervalMs);
        }
    };
    
    /**
     * Update visible POIs
     */
    private void updateVisiblePOIs() {
        if (!digiLensManager.isConnected()) {
            return;
        }
        
        // Get self location (user's current position)
        GeoPoint selfLocation = mapView.getSelfMarker().getPoint();
        if (selfLocation == null) {
            return;
        }
        
        // Process all map items
        for (MapItem item : mapView.getRootGroup().getItems()) {
            if (item instanceof PointMapItem) {
                PointMapItem poi = (PointMapItem) item;
                
                // Check if within range
                double distance = poi.getPoint().distanceTo(selfLocation);
                if (distance <= maxDistanceMeters) {
                    // Get POI details
                    String id = poi.getUID();
                    String name = poi.getMetaString("callsign", poi.getTitle());
                    GeoPoint point = poi.getPoint();
                    String color = getColorForItem(poi);
                    
                    // Send to DigiLens
                    digiLensManager.sendPOIData(
                        id,
                        name,
                        point.getLatitude(),
                        point.getLongitude(),
                        (float) point.getAltitude(),
                        color
                    );
                }
            }
        }
    }
    
    /**
     * Get color for a map item
     */
    private String getColorForItem(PointMapItem item) {
        // Default color (yellow)
        String color = "#FFFF00";
        
        // Determine color based on item type
        if (item instanceof Marker) {
            Marker marker = (Marker) item;
            if (marker.getType().equals("a-f-G")) {
                // Friendly
                color = "#0000FF"; // Blue
            } else if (marker.getType().equals("a-h-G")) {
                // Hostile
                color = "#FF0000"; // Red
            } else if (marker.getType().equals("a-n-G")) {
                // Neutral
                color = "#00FF00"; // Green
            } else if (marker.getType().equals("a-u-G")) {
                // Unknown
                color = "#FFFF00"; // Yellow
            }
        }
        
        return color;
    }
    
    @Override
    public void onMapEvent(MapEvent event) {
        // Force update when map items change
        handler.removeCallbacks(updatePOIsRunnable);
        handler.post(updatePOIsRunnable);
    }
}
