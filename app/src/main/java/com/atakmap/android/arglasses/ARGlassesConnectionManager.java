package com.atakmap.android.arglasses;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages connections to AR Glasses devices.
 * Supports both Bluetooth and TCP/IP connections.
 */
public class ARGlassesConnectionManager {

    private static final String TAG = "ARGlassesConnectionManager";
    
    // UUID for Bluetooth Serial Port Profile (SPP)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Default TCP port
    private static final int DEFAULT_TCP_PORT = 8080;
    
    private final MapView mapView;
    private final Context pluginContext;
    
    // Connection objects
    private BluetoothSocket bluetoothSocket;
    private Socket tcpSocket;
    private OutputStream outputStream;
    
    // Thread pool for connection operations
    private final ExecutorService executor;
    
    // Connection state
    private boolean connected = false;
    private String connectedDeviceAddress;
    private boolean isBluetoothConnection;
    
    /**
     * Constructor
     * @param mapView The map view
     * @param context The plugin context
     */
    public ARGlassesConnectionManager(MapView mapView, Context context) {
        this.mapView = mapView;
        this.pluginContext = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Connect to a device
     * @param deviceAddress Device address (Bluetooth MAC or IP address)
     * @return true if connection succeeds
     */
    public boolean connect(final String deviceAddress) {
        // Check if already connected
        if (connected && deviceAddress.equals(connectedDeviceAddress)) {
            Log.d(TAG, "Already connected to " + deviceAddress);
            return true;
        }
        
        // Disconnect if connected to a different device
        if (connected) {
            disconnect();
        }
        
        // Determine connection type (Bluetooth or TCP)
        isBluetoothConnection = isBluetoothAddress(deviceAddress);
        
        if (isBluetoothConnection) {
            return connectBluetooth(deviceAddress);
        } else {
            return connectTCP(deviceAddress);
        }
    }
    
    /**
     * Connect via Bluetooth
     * @param deviceAddress Bluetooth MAC address
     * @return true if connection succeeds
     */
    private boolean connectBluetooth(String deviceAddress) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            
            // Check if Bluetooth is available and enabled
            if (bluetoothAdapter == null) {
                showToast("Bluetooth is not available on this device");
                return false;
            }
            
            if (!bluetoothAdapter.isEnabled()) {
                showToast("Bluetooth is not enabled");
                return false;
            }
            
            // Get the Bluetooth device
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            
            // Create the socket and connect
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            
            // Connect on a background thread
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        
                        // Update connection state
                        connected = true;
                        connectedDeviceAddress = deviceAddress;
                        
                        Log.d(TAG, "Connected to Bluetooth device: " + deviceAddress);
                        showToast("Connected to Bluetooth device");
                    } catch (IOException e) {
                        Log.e(TAG, "Bluetooth connection failed", e);
                        showToast("Bluetooth connection failed: " + e.getMessage());
                        
                        // Clean up
                        try {
                            bluetoothSocket.close();
                        } catch (IOException closeException) {
                            Log.e(TAG, "Error closing socket", closeException);
                        }
                        bluetoothSocket = null;
                        outputStream = null;
                        connected = false;
                    }
                }
            });
            
            return true; // Return true to indicate connection attempt started
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Bluetooth connection", e);
            showToast("Bluetooth connection error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Connect via TCP/IP
     * @param deviceAddress IP address (can include port as "ip:port")
     * @return true if connection succeeds
     */
    private boolean connectTCP(final String deviceAddress) {
        try {
            // Parse address and port
            String host = deviceAddress;
            int port = DEFAULT_TCP_PORT;
            
            if (deviceAddress.contains(":")) {
                String[] parts = deviceAddress.split(":");
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid port number, using default");
                }
            }
            
            // Store final values for use in the thread
            final String finalHost = host;
            final int finalPort = port;
            
            // Connect on a background thread
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tcpSocket = new Socket(finalHost, finalPort);
                        outputStream = tcpSocket.getOutputStream();
                        
                        // Update connection state
                        connected = true;
                        connectedDeviceAddress = deviceAddress;
                        
                        Log.d(TAG, "Connected to TCP device: " + finalHost + ":" + finalPort);
                        showToast("Connected to TCP device");
                    } catch (IOException e) {
                        Log.e(TAG, "TCP connection failed", e);
                        showToast("TCP connection failed: " + e.getMessage());
                        
                        // Clean up
                        try {
                            if (tcpSocket != null) {
                                tcpSocket.close();
                            }
                        } catch (IOException closeException) {
                            Log.e(TAG, "Error closing socket", closeException);
                        }
                        tcpSocket = null;
                        outputStream = null;
                        connected = false;
                    }
                }
            });
            
            return true; // Return true to indicate connection attempt started
        } catch (Exception e) {
            Log.e(TAG, "Error setting up TCP connection", e);
            showToast("TCP connection error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnect from the device
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        // Disconnect on a background thread
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                        outputStream = null;
                    }
                    
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                        bluetoothSocket = null;
                    }
                    
                    if (tcpSocket != null) {
                        tcpSocket.close();
                        tcpSocket = null;
                    }
                    
                    connected = false;
                    connectedDeviceAddress = null;
                    
                    Log.d(TAG, "Disconnected from device");
                    showToast("Disconnected from device");
                } catch (IOException e) {
                    Log.e(TAG, "Error during disconnect", e);
                }
            }
        });
    }
    
    /**
     * Send data to the connected device
     * @param data The data to send
     * @return true if data was sent successfully
     */
    public boolean sendData(final byte[] data) {
        if (!isConnected() || outputStream == null) {
            return false;
        }
        
        // Send data on a background thread
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    outputStream.write(data);
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Error sending data", e);
                    
                    // Connection might be broken, disconnect
                    disconnect();
                }
            }
        });
        
        return true;
    }
    
    /**
     * Check if connected to a device
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Get the connected device address
     * @return The device address, or null if not connected
     */
    public String getConnectedDeviceAddress() {
        return connectedDeviceAddress;
    }
    
    /**
     * Check if the current connection is Bluetooth
     * @return true if Bluetooth, false if TCP/IP
     */
    public boolean isBluetoothConnection() {
        return isBluetoothConnection;
    }
    
    /**
     * Dispose of resources
     */
    public void dispose() {
        // Disconnect if connected
        disconnect();
        
        // Shut down the executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    /**
     * Check if the address is a Bluetooth MAC address
     * @param address The address to check
     * @return true if it's a Bluetooth address
     */
    private boolean isBluetoothAddress(String address) {
        // Bluetooth MAC addresses follow the format XX:XX:XX:XX:XX:XX
        // where X is a hexadecimal digit
        return address.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");
    }
    
    /**
     * Show a toast message on the UI thread
     * @param message The message to show
     */
    private void showToast(final String message) {
        if (mapView != null) {
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(pluginContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
