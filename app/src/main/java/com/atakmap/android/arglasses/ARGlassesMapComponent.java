package com.atakmap.android.arglasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.atakmap.android.arglasses.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.dropdown.DropDownMapComponent;

/**
 * Map component for AR Glasses plugin.
 * Responsible for registering the dropdown receivers and handling plugin initialization.
 */
public class ARGlassesMapComponent extends DropDownMapComponent {

    private static final String TAG = "ARGlassesMapComponent";
    
    private Context pluginContext;
    private ARGlassesDropDownReceiver dropDownReceiver;
    private ARGlassesConnectionManager connectionManager;
    private Intent dataServiceIntent;

    /**
     * Constructor for map component
     */
    public ARGlassesMapComponent() {
        super();
    }

    /**
     * Called when the plugin is created
     * @param context The plugin context
     * @param intent The intent used to create the plugin
     * @param view The map view
     */
    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        this.pluginContext = context;
        
        // Create the connection manager
        connectionManager = new ARGlassesConnectionManager(view, context);
        
        // Register the main dropdown UI
        dropDownReceiver = new ARGlassesDropDownReceiver(view, context, connectionManager);
        Log.d(TAG, "Registering AR Glasses plugin dropdown");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(ARGlassesDropDownReceiver.SHOW_PLUGIN, 
                "Shows the AR Glasses plugin dropdown");
        registerDropDownReceiver(dropDownReceiver, ddFilter);
        
        // Start the data service
        startDataService(context);
        
        Log.d(TAG, "AR Glasses plugin created");
    }

    /**
     * Called when the plugin is destroyed
     * @param context The plugin context
     * @param view The map view
     */
    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        // Stop the data service if it's running
        if (dataServiceIntent != null) {
            context.stopService(dataServiceIntent);
            dataServiceIntent = null;
        }
        
        // Clean up the connection manager
        if (connectionManager != null) {
            connectionManager.dispose();
            connectionManager = null;
        }
        
        super.onDestroyImpl(context, view);
        Log.d(TAG, "AR Glasses plugin destroyed");
    }
    
    /**
     * Starts the AR Glasses data service
     * @param context The plugin context
     */
    private void startDataService(Context context) {
        // Start the service to handle data transmission in the background
        dataServiceIntent = new Intent(context, ARGlassesDataService.class);
        
        // Load settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceAddress = prefs.getString("device_address", "");
        int updateRate = prefs.getInt("update_rate", 500);
        boolean enablePoi = prefs.getBoolean("enable_poi", true);
        boolean enableMap = prefs.getBoolean("enable_map", true);
        boolean enableCompass = prefs.getBoolean("enable_compass", true);
        
        // Add settings to the intent
        Bundle extras = new Bundle();
        extras.putString("device_address", deviceAddress);
        extras.putInt("update_rate", updateRate);
        extras.putBoolean("enable_poi", enablePoi);
        extras.putBoolean("enable_map", enableMap);
        extras.putBoolean("enable_compass", enableCompass);
        dataServiceIntent.putExtras(extras);
        
        // Start the service
        context.startService(dataServiceIntent);
        Log.d(TAG, "Started AR Glasses data service");
    }
}
