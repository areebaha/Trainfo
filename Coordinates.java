package edu.vassar.cmpu203.myfirstapplication.Model;

import java.util.Objects;

/**
 * Coordinates is a class that represents the coordinates of a specific location and station.
 */
public class Coordinates implements java.io.Serializable{
    private final double latitude;
    private final double longitude;

    /**
     * Constructor for Coordinates.
     * @param latitude
     * @param longitude
     */
    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Uses Wikipedia to see details of Haversine Formula and calculating distance between 2 coordinates.
     * Uses stackoverflow to see implementation of Haversine Formula.
     * <a href="https://stackoverflow.com/questions/365826/calculate-distance-between-2-gps-coordinates">...</a>
     */
    /**
     * distanceInKm method calculates the distance between 2 coordinates using the Haversine Formula.
     * @param coordA
     * @param coordB
     * @return distance between 2 coordinates
     */
    public static double distanceInKm(Coordinates coordA, Coordinates coordB) {
        double lat1Radians = Math.toRadians(coordA.getLatitude());
        double lat2Radians = Math.toRadians(coordB.getLatitude());
        double earthRadiusKm = 6371;

        double distanceLat = Math.toRadians(coordB.getLatitude() - coordA.getLatitude());
        double distanceLong = Math.toRadians(coordB.getLongitude() - coordA.getLongitude());

        double haversineFormula = Math.sin(distanceLat / 2) * Math.sin(distanceLat / 2) +
                Math.sin(distanceLong / 2) * Math.sin(distanceLong / 2) * Math.cos(lat1Radians) * Math.cos(lat2Radians);

        double angleCalculation = 2 * Math.atan2(Math.sqrt(haversineFormula), Math.sqrt(1 - haversineFormula));
        return earthRadiusKm * angleCalculation;
    }

    /**
     * equals method checks if 2 coordinates are the same.
     * @param o
     * @return boolean showing if 2 coordinates are the same.
     */
    @Override
    public boolean equals(Object o) {
        // If it's an identical object, return true
        if (this == o) return true;
        // Cast the object as Coordinates
        if (!(o instanceof Coordinates)) return false;
        Coordinates that = (Coordinates) o;
        // Check latitude and longitude
        return latitude == that.latitude && longitude == that.longitude;
    }

    /**
     * hashCode method returns the hashcode of the coordinates.
     * @return hashcode of the coordinates.
     */
    @Override
    public int hashCode() {
        // Use the hashcode of the latitude and longitude
        return Objects.hash(latitude, longitude);
    }

    /**
     * toString method for Coordinates.
     * @return
     */
    @Override
    public String toString() {
        return "Model.Coordinates{" +
                ", latitude=" + latitude +
                "longitude=" + longitude +
                '}';
    }
}
