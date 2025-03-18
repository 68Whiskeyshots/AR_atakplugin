package com.atakmap.android.apex.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;

/**
 * APEX plugin tool that appears in the ATAK toolbar
 */
public class APEXPluginTool extends AbstractPluginTool {
    
    /**
     * Constructor
     * @param context The plugin context
     */
    public APEXPluginTool(final Context context) {
        super(context, 
              context.getString(R.string.app_name), 
              context.getString(R.string.app_name),
              context.getResources().getDrawable(R.drawable.ic_launcher),
              "com.atakmap.android.apex.SHOW_PLUGIN");
        
        // Initialize native loader if needed
        APEXNativeLoader.init(context);
    }
}
