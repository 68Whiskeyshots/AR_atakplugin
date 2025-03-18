package com.atakmap.android.apex.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.apex.APEXMapComponent;
import com.atakmap.android.maps.MapComponent;

import gov.tak.api.plugin.IServiceController;

/**
 * Main entry point for the APEX plugin
 */
public class APEXLifeCycle extends AbstractPlugin {
    
    /**
     * Constructor for the plugin lifecycle implementation
     * @param isc the service controller provided by ATAK
     */
    public APEXLifeCycle(IServiceController isc) {
        super(isc, 
            new APEXPluginTool(((PluginContextProvider) isc.getService(PluginContextProvider.class)).getPluginContext()),
            (MapComponent) new APEXMapComponent());
    }
}
