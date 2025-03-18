// app/src/main/java/com/atakmap/android/apex/DigiLensConnectionManager.java
package com.atakmap.android.apex;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages connection to DigiLens AR glasses
 */
public class DigiLensConnectionManager {
    private static final String TAG = "DigiLensConnectionManager";
    
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    
    private Socket socket;
    private PrintWriter out;
    private boolean isConnected = false;
    private String ipAddress;
    private int port = 8088;
    
    private ConnectionListener connectionListener;
    
    /**
     * Interface for connection status updates
     */
    public interface ConnectionListener {
        void onConnectionStatusChanged(boolean connected);
    }
    
    /**
     * Constructor
     */
    public DigiLensConnectionManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Set a connection listener
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    /**
     * Connect to DigiLens glasses
     */
    public boolean connect(String ipAddress, int port) {
        if (isConnected) {
            return true;
        }
        
        this.ipAddress = ipAddress;
        this.port = port;
        
        executorService.execute(() -> {
            try {
                // Create socket connection
                socket = new Socket(ipAddress, port);
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                
                // Update connection status
                mainHandler.post(() -> {
                    isConnected = true;
                    if (connectionListener != null) {
                        connectionListener.onConnectionStatusChanged(true);
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to DigiLens glasses", e);
                mainHandler.post(() -> {
                    Toast.makeText(context, 
                                 "Connection error: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        return true; // Return true indicating we started the connection process
    }
    
    /**
     * Disconnect from DigiLens glasses
     */
    public void disconnect() {
        if (!isConnected) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error disconnecting from DigiLens glasses", e);
            } finally {
                mainHandler.post(() -> {
                    isConnected = false;
                    if (connectionListener != null) {
                        connectionListener.onConnectionStatusChanged(false);
                    }
                });
            }
        });
    }
    
    /**
     * Check if connected to DigiLens glasses
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    /**
     * Send POI data to DigiLens glasses
     */
    public void sendPOIData(String id, String name, double lat, double lon, float alt, String color) {
        if (!isConnected || out == null) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "poi");
                json.put("id", id);
                json.put("name", name);
                json.put("lat", lat);
                json.put("lon", lon);
                json.put("alt", alt);
                json.put("color", color);
                
                String jsonStr = json.toString();
                out.println(jsonStr);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating POI JSON", e);
            }
        });
    }
    
    /**
     * Send compass data to DigiLens glasses
     */
    public void sendCompassData(float heading) {
        if (!isConnected || out == null) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "compass");
                json.put("heading", heading);
                
                String jsonStr = json.toString();
                out.println(jsonStr);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating compass JSON", e);
            }
        });
    }
    
    /**
     * Shutdown the connection manager
     */
    public void shutdown() {
        disconnect();
        executorService.shutdown();
    }
}
