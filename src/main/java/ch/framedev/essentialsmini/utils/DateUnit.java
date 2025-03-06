package ch.framedev.essentialsmini.utils;

/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts ändern, @Copyright by FrameDev
 */
public enum DateUnit {

    SEC("Second(s)", 1),
    MIN("Minute(s)", 60),
    HOUR("Hour(s)", 60 * 60),
    DAY("Day(s)", 24 * 60 * 60),
    WEEK("Week(s)", 7 * 24 * 60 * 60),
    MON("Month(s)", 30 * 24 * 60 * 60),
    YEAR("Year(s)", 365 * 24 * 60 * 60);

    private final String output;

    private final long toSec;

    /**
     *
     */
    private DateUnit(String output, long toSec) {
        this.output = output;
        this.toSec = toSec;
    }

    /**
     * Convert toSeconds to Milliseconds
     *
     * @return returns the selected DateUnit to MilliSeconds
     */
    public long toMillis() {
        return toSec * 1000;
    }

    public long getToSec() {
        return toSec;
    }

    public String getOutput() {
        return output;
    }
}
