package com.atakmap.android.arglasses.plugin;

import android.content.Context;

import com.atakmap.coremap.log.Log;

import java.io.File;

/**
 * Utility class for loading native libraries.
 * This is used if the plugin needs to use native code.
 */
public class ARGlassesNativeLoader {

    private static final String TAG = "ARGlassesNativeLoader";
    private static String nativeLibraryDir = null;

    /**
     * Initialize the native loader
     * @param context The plugin context
     */
    synchronized public static void init(final Context context) {
        if (nativeLibraryDir == null) {
            try {
                nativeLibraryDir = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), 0)
                        .nativeLibraryDir;
                
                Log.d(TAG, "Native library directory: " + nativeLibraryDir);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get native library directory", e);
                throw new IllegalArgumentException(
                        "Native library loading will fail, unable to get the nativeLibraryDir from the package name");
            }
        }
    }

    /**
     * Load a native library
     * @param name The library name without the "lib" prefix or ".so" suffix
     */
    public static void loadLibrary(final String name) {
        if (nativeLibraryDir != null) {
            final String lib = nativeLibraryDir + File.separator
                    + System.mapLibraryName(name);
            
            if (new File(lib).exists()) {
                Log.d(TAG, "Loading native library: " + lib);
                System.load(lib);
            } else {
                Log.e(TAG, "Native library not found: " + lib);
                throw new IllegalArgumentException("Native library not found: " + lib);
            }
        } else {
            Log.e(TAG, "Native loader not initialized");
            throw new IllegalArgumentException("Native loader not initialized");
        }
    }
}
