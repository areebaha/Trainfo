package edu.vassar.cmpu203.myfirstapplication.Model;

/**
 * Destination is a class that uses a Coordinate coords and a name and is used to find the specific
 * location that a user types in.
 */
public class Destination {
    private final Coordinates coords;
    private final String name;

    /**
     * Constructor for Destination.
     * @param coords
     * @param name
     */
    public Destination(Coordinates coords, String name) {
        this.coords = coords;
        this.name = name;
    }

    public Coordinates getCoords() {
        return coords;
    }

    public String getName() {
        return name;
    }

    /**
     * toString method for Destination.
     * @return
     */
    @Override
    public String toString() {
        return "Model.Destination{" +
                "coords=" + coords +
                ", name='" + name + '\'' +
                '}';
    }
}
