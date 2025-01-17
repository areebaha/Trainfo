package edu.vassar.cmpu203.myfirstapplication.Model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;

/**
 * ClockTime is a class that represents the ETA of a specific train in a route and will be used in
 * the next phase
 */
public class ClockTime {
    private final int hours;
    private final int minutes;
    private final int seconds;

    /**
     * Constructor for ClockTime.
     * @param hours The hours; should be between 0-24
     * @param minutes The minutes; should be between 0-60
     * @param seconds The seconds; should be between 0-60
     */
    public ClockTime(int hours, int minutes, int seconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    public ClockTime(String isoDateString) {
        // Convert date string to date. We use UTC as the timezone
        // because it's the standard for ISO dates.
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.parse(isoDateString), ZoneOffset.UTC);
        // Convert to ClockTime
        this.hours = dateTime.getHour();
        this.minutes = dateTime.getMinute();
        this.seconds = dateTime.getSecond();
    }

    /**
     * Getter for the hours. Should be between 0-24
     */
    public int getHours() {
        return hours;
    }

    /**
     * Getter for the minutes. Should be between 0-60
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * Getter for the seconds. Should be between 0-60
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Gets the duration in minutes between two ClockTimes.
     * @param start The start time.
     * @param end The end time.
     * @return The duration in minutes.
     */
    public static int minuteDuration(ClockTime start, ClockTime end) {
        return (end.hours - start.hours) * 60 + (end.minutes - start.minutes);
    }

    /**
     * Converts ClockTime to military time without displaying seconds.
     */
    public String toMilitaryTime() {
        return String.format(Locale.US, "%02d:%02d", hours, minutes);
    }

    /**
     * toString method for ClockTime.
     * @return
     */
    @Override
    public String toString() {
        return "{ " + hours + ":" + minutes + "," + seconds + " }";
    }

    /**
     * Equals method for ClockTime.
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ClockTime other = (ClockTime) obj;
        return other.hours == hours && other.minutes == minutes && other.seconds == seconds;
    }

    /**
     * HashCode method for ClockTime.
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(hours, minutes, seconds);
    }
}
