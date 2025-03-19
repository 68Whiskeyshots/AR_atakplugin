package com.atakmap.android.arglasses.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.arglasses.ARGlassesMapComponent;

import gov.tak.api.plugin.IServiceController;

/**
 * Main plugin lifecycle entry point for the AR Glasses plugin.
 * Handles plugin initialization and shutdown.
 */
public class ARGlassesLifecycle extends AbstractPlugin {

    /**
     * Constructor for the lifecycle class
     * @param isc The service controller provided by ATAK
     */
    public ARGlassesLifecycle(IServiceController isc) {
        super(isc, 
              // Create plugin tool with plugin context
              new ARGlassesPluginTool(((PluginContextProvider) isc.getService(
                      PluginContextProvider.class)).getPluginContext()),
              // Create map component 
              (MapComponent) new ARGlassesMapComponent());
    }
}
