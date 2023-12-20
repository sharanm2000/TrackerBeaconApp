package com.ram.tracker;

public class Vehicles {

    private String name;
    private double latitude;
    private double longitude;
    private boolean path;
    private double origin_latitude;
    private double origin_longitude;
    private double destination_latitude;
    private double destination_longitude;

    public Vehicles() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean getPath() {
        return path;
    }

    public void setPath(boolean path) {
        this.path = path;
    }

    public double getOrigin_latitude() {
        return origin_latitude;
    }

    public void setOrigin_latitude(double origin_latitude) {
        this.origin_latitude = origin_latitude;
    }

    public double getOrigin_longitude() {
        return origin_longitude;
    }

    public void setOrigin_longitude(double origin_longitude) {
        this.origin_longitude = origin_longitude;
    }

    public double getDestination_latitude() {
        return destination_latitude;
    }

    public void setDestination_latitude(double destination_latitude) {
        this.destination_latitude = destination_latitude;
    }

    public double getDestination_longitude() {
        return destination_longitude;
    }

    public void setDestination_longitude(double destination_longitude) {
        this.destination_longitude = destination_longitude;
    }



}
