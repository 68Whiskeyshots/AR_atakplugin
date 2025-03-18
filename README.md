# ATAK-Plugin-Template
plugin template for ATAK 5.1.0.x

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
