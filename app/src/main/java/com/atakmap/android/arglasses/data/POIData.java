package com.atakmap.android.arglasses.data;

import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Data class that represents a Point of Interest (POI) on the map.
 * This is used to pass POI data to the AR glasses.
 */
public class POIData {
    
    private final String id;
    private final String name;
    private final String type;
    private final GeoPoint location;
    private final int color;
    
    /**
     * Constructor
     * @param id The unique identifier
     * @param name The display name
     * @param type The type of POI
     * @param location The geographic location
     * @param color The display color
     */
    public POIData(String id, String name, String type, GeoPoint location, int color) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.color = color;
    }
    
    /**
     * Get the unique identifier
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the display name
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the type of POI
     * @return The type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the geographic location
     * @return The location as a GeoPoint
     */
    public GeoPoint getLocation() {
        return location;
    }
    
    /**
     * Get the display color
     * @return The color as an integer
     */
    public int getColor() {
        return color;
    }
    
    @Override
    public String toString() {
        return "POIData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", location=" + location +
                '}';
    }
}
