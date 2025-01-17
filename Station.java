package edu.vassar.cmpu203.myfirstapplication.Model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A class representing a station. It should have the same
 * coordinates as a GTFS station (represented by the StationDetails class)
 */
public class Station {
    private final String name;
    private final Coordinates coords;
    private final String isAccessible;
    private final String borough;
    public final String accessibilityNote;


    /**
     * Constructor for Station.
     * @param name
     * @param coords
     * @param isAccessible
     * @param borough
     */
    public Station(String name, Coordinates coords, String isAccessible, String borough
    , String accessibilityNote) {
        this.name = name;
        this.coords = coords;
        this.isAccessible = isAccessible;
        this.borough = borough;
        this.accessibilityNote = accessibilityNote;
    }

    /**
     * Get the name of the station.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the coordinates of the station.
     * @return
     */
    public Coordinates getCoords() {
        return coords;
    }

    /**
     * Check if the station is accessible.
     * @return
     */
    public boolean isAccessible() {
        return isAccessible.equals("1");
    }

    public String isAccessibleString() {
        if (isAccessible.equals("2")) {
            return "Partially Accessible: ";
        } else if (isAccessible()) {
            return "Accessible";
        } else {
            return "Not Accessible";
        }
    }

    public String getBorough() {
        if (Objects.equals(borough, "Bk")) {
            return "Brooklyn";
        }
        else if (Objects.equals(borough, "Bx")) {
            return "Bronx";
        }
        else if (Objects.equals(borough, "M")) {
            return "Manhattan";
        }
        else if (Objects.equals(borough, "Q")) {
            return "Queens";
        }
        else if (Objects.equals(borough, "SI")) {
            return "Staten Island";
        }
        else {
            return "Unknown";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Station station = (Station) obj;
        return name.equals(station.name) &&
                coords.equals(station.coords) &&
                isAccessible.equals(station.isAccessible) &&
                borough.equals(station.borough) &&
                accessibilityNote.equals(station.accessibilityNote);
    }

    /**
     * Returns a string representation of the station.
     * @return station in String format
     */
    @NonNull
    public String toString() {
        return name + " ("
                + coords.getLatitude() +
                ", " + coords.getLongitude() + "), "
                + isAccessibleString() + ", "
                + getBorough() + ", "
                + accessibilityNote;

    }
}
