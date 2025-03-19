package com.atakmap.android.arglasses;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.atakmap.android.arglasses.data.ARGlassesDataProvider;
import com.atakmap.android.arglasses.data.POIData;
import com.atakmap.android.arglasses.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service that provides data to AR glasses.
 * This service runs in the background and sends data to the connected device at regular intervals.
 */
public class ARGlassesDataService extends Service {

    private static final String TAG = "ARGlassesDataService";
    
    // Notification settings
    private static final String NOTIFICATION_CHANNEL_ID = "com.atakmap.android.arglasses.service";
    private static final int NOTIFICATION_ID = 8675309;
    
    // Actions
    public static final String ACTION_UPDATE_SETTINGS = "com.atakmap.android.arglasses.UPDATE_SETTINGS";
    
    // Data feed settings
    private ARGlassesConnectionManager connectionManager;
    private ARGlassesDataProvider dataProvider;
    private String deviceAddress = "";
    private int updateRate = 500; // Default update rate (ms)
    private boolean enablePoi = true;
    private boolean enableMap = true;
    private boolean enableCompass = true;
    
    // Timer for data updates
    private Timer updateTimer;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Handler for UI thread
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AR Glasses data service created");
        
        // Create the data provider
        dataProvider = new ARGlassesDataProvider(this);
        
        // Create the connection manager
        MapView mapView = MapView.getMapView();
        if (mapView != null) {
            connectionManager = new ARGlassesConnectionManager(mapView, this);
        } else {
            Log.e(TAG, "MapView is null, service cannot function properly");
            stopSelf();
            return;
        }
        
        // Create notification channel for Android O+
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        
        // Handle action to update settings
        if (ACTION_UPDATE_SETTINGS.equals(intent.getAction())) {
            updateSettings(intent);
        } else {
            // Get settings from intent
            deviceAddress = intent.getStringExtra("device_address");
            updateRate = intent.getIntExtra("update_rate", 500);
            enablePoi = intent.getBooleanExtra("enable_poi", true);
            enableMap = intent.getBooleanExtra("enable_map", true);
            enableCompass = intent.getBooleanExtra("enable_compass", true);
            
            // Start the service in the foreground
            startForeground(NOTIFICATION_ID, createNotification());
            
            // Start data feed if device address is set
            if (deviceAddress != null && !deviceAddress.isEmpty()) {
                startDataFeed();
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        // Stop the data feed
        stopDataFeed();
        
        // Disconnect from device
        if (connectionManager != null) {
            connectionManager.disconnect();
            connectionManager.dispose();
            connectionManager = null;
        }
        
        Log.d(TAG, "AR Glasses data service destroyed");
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    /**
     * Update service settings from intent
     * @param intent The intent with settings
     */
    private void updateSettings(Intent intent) {
        // Get settings from intent
        String newDeviceAddress = intent.getStringExtra("device_address");
        int newUpdateRate = intent.getIntExtra("update_rate", 500);
        boolean newEnablePoi = intent.getBooleanExtra("enable_poi", true);
        boolean newEnableMap = intent.getBooleanExtra("enable_map", true);
        boolean newEnableCompass = intent.getBooleanExtra("enable_compass", true);
        
        // Check if device address has changed
        boolean deviceChanged = newDeviceAddress != null && 
                               !newDeviceAddress.equals(deviceAddress);
        
        // Update settings
        deviceAddress = newDeviceAddress != null ? newDeviceAddress : deviceAddress;
        updateRate = newUpdateRate;
        enablePoi = newEnablePoi;
        enableMap = newEnableMap;
        enableCompass = newEnableCompass;
        
        // Update the notification
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification());
        }
        
        // Restart data feed if required
        if (isRunning.get()) {
            // Reconnect if device changed
            if (deviceChanged) {
                stopDataFeed();
                startDataFeed();
            } else {
                // Just restart the timer if only update rate changed
                restartTimer();
            }
        } else if (deviceAddress != null && !deviceAddress.isEmpty()) {
            // Start data feed if not running
            startDataFeed();
        }
    }
    
    /**
     * Start the data feed to the connected device
     */
    private void startDataFeed() {
        // Check if already running
        if (isRunning.getAndSet(true)) {
            return;
        }
        
        // Connect to the device
        if (!connectionManager.isConnected() && !connectionManager.connect(deviceAddress)) {
            // Connection failed, don't start the data feed
            isRunning.set(false);
            return;
        }
        
        // Start periodic data updates
        startTimer();
        
        // Show a toast message
        showToast(getString(R.string.sending_data));
        Log.d(TAG, "Started data feed to " + deviceAddress);
    }
    
    /**
     * Stop the data feed
     */
    private void stopDataFeed() {
        // Check if running
        if (!isRunning.getAndSet(false)) {
            return;
        }
        
        // Stop the timer
        stopTimer();
        
        // Disconnect from the device
        if (connectionManager != null && connectionManager.isConnected()) {
            connectionManager.disconnect();
        }
        
        // Show a toast message
        showToast(getString(R.string.stopping_data));
        Log.d(TAG, "Stopped data feed");
    }
    
    /**
     * Start the timer for periodic data updates
     */
    private void startTimer() {
        // Cancel existing timer if any
        stopTimer();
        
        // Create new timer
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRunning.get() && connectionManager != null && connectionManager.isConnected()) {
                    sendData();
                }
            }
        }, 0, updateRate);
    }
    
    /**
     * Restart the timer with the current update rate
     */
    private void restartTimer() {
        if (isRunning.get()) {
            startTimer();
        }
    }
    
    /**
     * Stop the timer
     */
    private void stopTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
    
    /**
     * Send data to the connected device
     */
    private void sendData() {
        try {
            // Create a JSON object with all data
            JSONObject data = new JSONObject();
            
            // Add timestamp
            data.put("timestamp", System.currentTimeMillis());
            
            // Add POI data if enabled
            if (enablePoi) {
                JSONArray poisArray = new JSONArray();
                List<POIData> pois = dataProvider.getPointsOfInterest();
                
                for (POIData poi : pois) {
                    JSONObject poiJson = new JSONObject();
                    poiJson.put("id", poi.getId());
                    poiJson.put("name", poi.getName());
                    poiJson.put("type", poi.getType());
                    poiJson.put("lat", poi.getLocation().getLatitude());
                    poiJson.put("lon", poi.getLocation().getLongitude());
                    poiJson.put("alt", poi.getLocation().getAltitude());
                    poiJson.put("color", poi.getColor());
                    poisArray.put(poiJson);
                }
                
                data.put("pois", poisArray);
            }
            
            // Add map data if enabled
            if (enableMap) {
                JSONObject mapData = new JSONObject();
                GeoPoint selfLocation = dataProvider.getSelfLocation();
                
                if (selfLocation != null) {
                    mapData.put("self_lat", selfLocation.getLatitude());
                    mapData.put("self_lon", selfLocation.getLongitude());
                    mapData.put("self_alt", selfLocation.getAltitude());
                }
                
                // Add map view settings
                mapData.put("zoom_level", dataProvider.getZoomLevel());
                mapData.put("map_bearing", dataProvider.getMapBearing());
                
                data.put("map", mapData);
            }
            
            // Add compass data if enabled
            if (enableCompass) {
                JSONObject compassData = new JSONObject();
                compassData.put("heading", dataProvider.getDeviceHeading());
                compassData.put("tilt", dataProvider.getDeviceTilt());
                compassData.put("roll", dataProvider.getDeviceRoll());
                
                data.put("compass", compassData);
            }
            
            // Convert to JSON string
            String jsonData = data.toString();
            
            // Send the data
            if (connectionManager != null && connectionManager.isConnected()) {
                connectionManager.sendData(jsonData.getBytes());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON data", e);
        }
    }
    
    /**
     * Create the notification channel (required for Android O+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AR Glasses Data Service";
            String description = "Background service for AR Glasses data feed";
            int importance = NotificationManager.IMPORTANCE_LOW;
            
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create the service notification
     * @return The notification
     */
    private Notification createNotification() {
        // Create a pending intent to launch the plugin dropdown
        Intent intent = new Intent();
        intent.setAction("com.atakmap.android.arglasses.SHOW_PLUGIN");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ar_glasses)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(connectionManager != null && connectionManager.isConnected() ? 
                        "Connected to " + deviceAddress : "Not connected")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        
        return builder.build();
    }
    
    /**
     * Show a toast message on the UI thread
     * @param message The message to show
     */
    private void showToast(final String message) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ARGlassesDataService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
