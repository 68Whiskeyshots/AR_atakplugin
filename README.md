# ATAK AR Glasses Plugin Project

## Overview

This plugin enables ATAK to serve as a data feed provider for augmented reality (AR) glasses. It extracts map information, points of interest, and device orientation data from ATAK and sends it to AR glasses devices via Bluetooth or TCP/IP connections. The plugin runs as a background service to maintain continuous data streaming even when the ATAK app is running in the background.

## Structure & Implementation

### Plugin Architecture

The plugin follows the standard ATAK plugin architecture:

1. **Lifecycle Class**: `ARGlassesLifecycle` - the main entry point for the plugin
2. **Plugin Tool**: `ARGlassesPluginTool` - creates the plugin icon in the ATAK toolbar
3. **Map Component**: `ARGlassesMapComponent` - initializes the plugin components and data service
4. **Dropdown Receiver**: `ARGlassesDropDownReceiver` - provides the user interface to configure the plugin
5. **Background Service**: `ARGlassesDataService` - collects and transmits data to AR glasses

### Key Features

- **Multiple Connection Methods**: Supports both Bluetooth SPP and TCP/IP connections
- **Configurable Data Feed**: Users can enable/disable different data channels (POIs, map, compass)
- **Adjustable Update Rate**: Configure how frequently data is sent to the AR glasses
- **Persistent Service**: Runs in the background to maintain data feed while ATAK is minimized
- **JSON Data Format**: Uses standard JSON format for easy parsing on the AR glasses side

### Data Types Provided

1. **Points of Interest (POIs)**: Map markers from ATAK including their position and properties
2. **Map Data**: Current map view settings, self-location, zoom level, and bearing
3. **Compass Information**: Device heading, tilt, and roll for orientation awareness

## Integration with AR Glasses

### Data Format

Data is sent in JSON format with this structure:

```json
{
  "timestamp": 1647372924000,
  "pois": [
    {
      "id": "unique-id-123",
      "name": "Marker Name",
      "type": "marker-type",
      "lat": 37.422,
      "lon": -122.084,
      "alt": 123.4,
      "color": -16711936
    }
  ],
  "map": {
    "self_lat": 37.422,
    "self_lon": -122.084,
    "self_alt": 123.4,
    "zoom_level": 16.5,
    "map_bearing": 45.0
  },
  "compass": {
    "heading": 45.0,
    "tilt": 10.2,
    "roll": 0.5
  }
}
```

### AR Glasses Requirements

For AR glasses to work with this plugin, they must:

1. Support Bluetooth SPP or TCP/IP connectivity
2. Be able to parse JSON data
3. Implement visualization logic for the received data types

## Building and Installing

### Prerequisites

- ATAK SDK 5.1.0 or later
- Android Studio
- Java 11 JDK

### Build Steps

1. Place the plugin in the appropriate SDK directory
2. Generate signing keys
3. Update the local.properties file with paths to signing keys
4. Build using Gradle: `./gradlew assembleCivDebug`

### Installation

1. Install the resulting APK on a device with ATAK
2. Launch ATAK
3. Access the plugin from the ATAK toolbar

## User Guide

### Initial Setup

1. Tap the AR Glasses icon in the ATAK toolbar
2. Enter the device address (Bluetooth MAC or IP address)
3. Configure update rate and data channels
4. Tap "Connect" to establish connection

### Configuration Options

- **Device Address**: Bluetooth MAC or IP address of the AR glasses
- **Update Rate**: How frequently data is sent (in milliseconds)
- **Data Channels**: Toggle which data types are sent (POIs, map, compass)

### Troubleshooting

- **Connection Issues**: Verify device address and ensure Bluetooth is enabled
- **Data Not Appearing**: Check that all required data channels are enabled
- **Service Stopped**: Reopen ATAK if the service was killed by the system

## Development and Extension

The plugin can be extended in several ways:

1. **Add New Data Types**: Extend `ARGlassesDataProvider` to collect additional data
2. **Support More Connection Methods**: Enhance `ARGlassesConnectionManager`
3. **Customize UI**: Modify `arglasses_main_layout.xml` to add more controls

## Technical Requirements

- Android 5.0 (API level 21) or higher
- ATAK 5.1.0 or higher
- Bluetooth or network connectivity

## Implementation Notes

- All network operations run on background threads to avoid UI blocking
- The plugin uses a foreground service with notification to ensure reliable operation
- Sensor data is collected directly from the device's sensors for accurate orientation information
APEX/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── plugin.xml                   # Plugin descriptor
│   │   ├── java/com/atakmap/android/apex/
│   │   │   ├── plugin/                      # Plugin lifecycle
│   │   │   │   ├── APEXLifeCycle.java
│   │   │   │   ├── APEXPluginTool.java
│   │   │   │   └── APEXNativeLoader.java
│   │   │   ├── APEXMapComponent.java        # Main map component
│   │   │   ├── APEXDropDownReceiver.java    # UI dropdown
│   │   │   ├── DigiLensConnectionManager.java  # Connection to glasses
│   │   │   ├── POIManager.java              # POI handling
│   │   │   └── CompassManager.java          # Compass handling
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   └── ic_launcher.xml
│   │   │   ├── layout/
│   │   │   │   └── apex_main_layout.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       ├── styles.xml
│   │   │       └── dimen.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle                         # App module build file
│   ├── proguard-rules.pro
│   └── proguard-gradle.txt
├── build.gradle                             # Project level build file
├── gradle.properties
└── settings.gradle
