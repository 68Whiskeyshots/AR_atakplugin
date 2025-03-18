// app/src/main/java/com/atakmap/android/apex/APEXDropDownReceiver.java
package com.atakmap.android.apex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.apex.plugin.R;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

/**
 * Drop down receiver for the APEX plugin
 */
public class APEXDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = "APEXDropDownReceiver";
    public static final String SHOW_PLUGIN = "com.atakmap.android.apex.SHOW_PLUGIN";
    
    private static final String PREFS_NAME = "APEXPluginPrefs";
    
    private final Context pluginContext;
    private final View mainView;
    private final DigiLensConnectionManager digiLensManager;
    private final POIManager poiManager;
    private final CompassManager compassManager;
    
    // UI elements
    private TextView connectionStatusText;
    private EditText ipAddressInput;
    private EditText portInput;
    private EditText maxDistanceInput;
    private Button connectButton;
    private Button disconnectButton;
    private Button saveSettingsButton;
    
    /**
     * Constructor
     */
    public APEXDropDownReceiver(MapView mapView, Context context, 
                               DigiLensConnectionManager digiLensManager,
                               POIManager poiManager,
                               CompassManager compassManager) {
        super(mapView);
        this.pluginContext = context;
        this.digiLensManager = digiLensManager;
        this.poiManager = poiManager;
        this.compassManager = compassManager;
        
        // Inflate the main layout
        mainView = PluginLayoutInflater.inflate(context, R.layout.apex_main_layout, null);
        
        // Initialize the UI
        initUI();
        
        // Set up connection status listener
        digiLensManager.setConnectionListener(connected -> updateConnectionStatus());
    }
    
    /**
     * Initialize UI elements and their listeners
     */
    private void initUI() {
        // Get UI elements
        connectionStatusText = mainView.findViewById(R.id.connection_status);
        ipAddressInput = mainView.findViewById(R.id.ip_address);
        portInput = mainView.findViewById(R.id.port);
        maxDistanceInput = mainView.findViewById(R.id.max_distance);
        connectButton = mainView.findViewById(R.id.connect_button);
        disconnectButton = mainView.findViewById(R.id.disconnect_button);
        saveSettingsButton = mainView.findViewById(R.id.save_settings_button);
        
        // Load saved preferences
        loadPreferences();
        
        // Set button listeners
        connectButton.setOnClickListener(v -> connectToGlasses());
        disconnectButton.setOnClickListener(v -> disconnectFromGlasses());
        saveSettingsButton.setOnClickListener(v -> saveSettings());
        
        // Update UI based on current connection status
        updateConnectionStatus();
    }
    
    /**
     * Load saved preferences
     */
    private void loadPreferences() {
        SharedPreferences prefs = pluginContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ipAddressInput.setText(prefs.getString("ip_address", ""));
        portInput.setText(String.valueOf(prefs.getInt("port", 8088)));
        maxDistanceInput.setText(String.valueOf(prefs.getInt("max_distance", 1000)));
    }
    
    /**
     * Save current settings
     */
    private void saveSettings() {
        String ipAddress = ipAddressInput.getText().toString().trim();
        int port;
        try {
            port = Integer.parseInt(portInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(pluginContext, "Invalid port number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int maxDistance;
        try {
            maxDistance = Integer.parseInt(maxDistanceInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(pluginContext, "Invalid max distance", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save to preferences
        SharedPreferences prefs = pluginContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ip_address", ipAddress);
        editor.putInt("port", port);
        editor.putInt("max_distance", maxDistance);
        editor.apply();
        
        // Update managers with new settings
        poiManager.setMaxDistanceMeters(maxDistance);
        
        Toast.makeText(pluginContext, "Settings saved", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Connect to DigiLens glasses
     */
    private void connectToGlasses() {
        String ipAddress = ipAddressInput.getText().toString().trim();
        if (ipAddress.isEmpty()) {
            Toast.makeText(pluginContext, "IP address is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(pluginContext, "Invalid port number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Try to connect
        if (digiLensManager.connect(ipAddress, port)) {
            // Start POI and compass managers
            poiManager.start();
            compassManager.start();
            
            updateConnectionStatus();
            Toast.makeText(pluginContext, "Connected to DigiLens glasses", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(pluginContext, "Failed to connect", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Disconnect from DigiLens glasses
     */
    private void disconnectFromGlasses() {
        // Stop managers
        poiManager.stop();
        compassManager.stop();
        
        // Disconnect
        digiLensManager.disconnect();
        
        updateConnectionStatus();
        Toast.makeText(pluginContext, "Disconnected from DigiLens glasses", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update the connection status UI
     */
    private void updateConnectionStatus() {
        boolean connected = digiLensManager.isConnected();
        connectionStatusText.setText(connected ? "Connected" : "Disconnected");
        connectionStatusText.setTextColor(connected ? 0xFF00FF00 : 0xFFFF0000);
        
        // Update button states
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
    }
    
    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
        if (v) {
            updateConnectionStatus();
        }
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void disposeImpl() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }
        
        if (action.equals(SHOW_PLUGIN)) {
            Log.d(TAG, "showing plugin drop down");
            showDropDown(mainView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        }
    }
}
