package com.atakmap.android.arglasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.arglasses.plugin.R;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

/**
 * Drop down receiver for AR Glasses plugin.
 * Handles the UI and user interaction.
 */
public class ARGlassesDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String TAG = "ARGlassesDropDownReceiver";
    public static final String SHOW_PLUGIN = "com.atakmap.android.arglasses.SHOW_PLUGIN";
    
    private final Context pluginContext;
    private final View mainView;
    private final ARGlassesConnectionManager connectionManager;
    
    // UI Elements
    private EditText deviceAddressInput;
    private EditText updateRateInput;
    private CheckBox enablePoiCheckbox;
    private CheckBox enableMapCheckbox;
    private CheckBox enableCompassCheckbox;
    private Button connectButton;
    private Button saveButton;
    private TextView connectionStatusText;

    /**
     * Constructor
     * @param mapView The map view
     * @param context The plugin context
     * @param connectionManager The connection manager
     */
    public ARGlassesDropDownReceiver(MapView mapView, Context context, 
                                    ARGlassesConnectionManager connectionManager) {
        super(mapView);
        this.pluginContext = context;
        this.connectionManager = connectionManager;
        
        // Inflate the layout
        mainView = PluginLayoutInflater.inflate(context, R.layout.arglasses_main_layout, null);
        
        // Initialize UI elements
        initializeUI();
        
        // Load saved settings
        loadSettings();
    }

    /**
     * Initialize UI elements and set up listeners
     */
    private void initializeUI() {
        deviceAddressInput = mainView.findViewById(R.id.device_address);
        updateRateInput = mainView.findViewById(R.id.update_rate);
        enablePoiCheckbox = mainView.findViewById(R.id.enable_poi);
        enableMapCheckbox = mainView.findViewById(R.id.enable_map);
        enableCompassCheckbox = mainView.findViewById(R.id.enable_compass);
        connectButton = mainView.findViewById(R.id.connect_button);
        saveButton = mainView.findViewById(R.id.save_settings);
        connectionStatusText = mainView.findViewById(R.id.connection_status);
        
        // Set up connection button click listener
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectionManager.isConnected()) {
                    // Disconnect if already connected
                    connectionManager.disconnect();
                    updateConnectionStatus(false);
                } else {
                    // Connect if not connected
                    String deviceAddress = deviceAddressInput.getText().toString().trim();
                    if (deviceAddress.isEmpty()) {
                        Toast.makeText(pluginContext, R.string.no_device_address,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Save settings before connecting
                    saveSettings();
                    
                    // Connect to device
                    connectionStatusText.setText(R.string.connecting);
                    connectionStatusText.setTextColor(Color.YELLOW);
                    
                    // Start connection process
                    boolean connected = connectionManager.connect(deviceAddress);
                    updateConnectionStatus(connected);
                    
                    if (connected) {
                        // Update service with new settings
                        updateService();
                    }
                }
            }
        });
        
        // Set up save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(pluginContext, "Settings saved", Toast.LENGTH_SHORT).show();
                
                // Update service with new settings
                updateService();
            }
        });
    }
    
    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pluginContext);
        
        // Load device address
        String deviceAddress = prefs.getString("device_address", "");
        deviceAddressInput.setText(deviceAddress);
        
        // Load update rate
        int updateRate = prefs.getInt("update_rate", 500);
        updateRateInput.setText(String.valueOf(updateRate));
        
        // Load data channel settings
        boolean enablePoi = prefs.getBoolean("enable_poi", true);
        boolean enableMap = prefs.getBoolean("enable_map", true);
        boolean enableCompass = prefs.getBoolean("enable_compass", true);
        
        enablePoiCheckbox.setChecked(enablePoi);
        enableMapCheckbox.setChecked(enableMap);
        enableCompassCheckbox.setChecked(enableCompass);
        
        // Check if already connected
        updateConnectionStatus(connectionManager.isConnected());
    }
    
    /**
     * Save settings to SharedPreferences
     */
    private void saveSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pluginContext);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save device address
        String deviceAddress = deviceAddressInput.getText().toString().trim();
        editor.putString("device_address", deviceAddress);
        
        // Save update rate
        String updateRateStr = updateRateInput.getText().toString().trim();
        int updateRate = 500; // Default
        try {
            updateRate = Integer.parseInt(updateRateStr);
            if (updateRate < 100) updateRate = 100; // Minimum 100ms
            if (updateRate > 5000) updateRate = 5000; // Maximum 5 seconds
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid update rate, using default");
        }
        editor.putInt("update_rate", updateRate);
        updateRateInput.setText(String.valueOf(updateRate));
        
        // Save data channel settings
        editor.putBoolean("enable_poi", enablePoiCheckbox.isChecked());
        editor.putBoolean("enable_map", enableMapCheckbox.isChecked());
        editor.putBoolean("enable_compass", enableCompassCheckbox.isChecked());
        
        // Apply changes
        editor.apply();
    }
    
    /**
     * Update the connection status UI
     * @param connected Whether connected or not
     */
    private
