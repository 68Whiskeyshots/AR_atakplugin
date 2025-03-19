package com.atakmap.android.arglasses.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.atak.plugins.impl.AbstractPluginTool;

/**
 * Plugin tool for the AR Glasses plugin.
 * Creates the icon in the ATAK toolbar.
 */
public class ARGlassesPluginTool extends AbstractPluginTool {

    /** 
     * Constructor for the plugin tool
     * @param context The plugin context
     */
    public ARGlassesPluginTool(final Context context) {
        super(context, 
              context.getString(R.string.app_name), 
              context.getString(R.string.app_desc),
              context.getResources().getDrawable(R.drawable.ar_glasses), 
              "com.atakmap.android.arglasses.SHOW_PLUGIN");
        
        // Initialize native libraries if needed
        ARGlassesNativeLoader.init(context);
    }

    /**
     * Get the icon for the plugin
     * @return The drawable icon
     */
    @Override
    public Drawable getIcon() {
        return (context == null) ? 
                null : 
                context.getResources().getDrawable(R.drawable.ar_glasses);
    }
}
