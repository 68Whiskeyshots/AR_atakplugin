// app/src/main/java/com/atakmap/android/apex/APEXMapComponent.java
package com.atakmap.android.apex;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.apex.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.coremap.log.Log;

/**
 * Main map component for the APEX plugin
 */
public class APEXMapComponent extends DropDownMapComponent {
    private static final String TAG = "APEXMapComponent";
    
    private Context pluginContext;
    private APEXDropDownReceiver dropDownReceiver;
    private DigiLensConnectionManager digiLensManager;
    private POIManager poiManager;
    private CompassManager compassManager;

    /**
     * onCreate is called when the MapComponent is created.
     * @param context the context
     * @param intent the intent
     * @param view the map view
     */
    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        
        pluginContext = context;
        
        Log.d(TAG, "onCreate");
        
        // Initialize managers
        digiLensManager = new DigiLensConnectionManager(pluginContext);
        poiManager = new POIManager(view, digiLensManager);
        compassManager = new CompassManager(view, digiLensManager);
        
        // Create dropdown receiver
        dropDownReceiver = new APEXDropDownReceiver(view, context, digiLensManager, poiManager, compassManager);
        
        // Register the dropdown receiver with ATAK
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(APEXDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(dropDownReceiver, filter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (digiLensManager != null) {
            digiLensManager.shutdown();
        }
        
        if (poiManager != null) {
            poiManager.stop();
        }
        
        if (compassManager != null) {
            compassManager.stop();
        }
        
        super.onDestroyImpl(context, view);
    }
}
