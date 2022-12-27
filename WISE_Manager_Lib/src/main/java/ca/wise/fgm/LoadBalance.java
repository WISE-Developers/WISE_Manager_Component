package ca.wise.fgm;

/**
 * The type of load balancing that should be used
 * for a job.
 */
public enum LoadBalance {

    /**
     * No load balancing is to be used.
     */
    NONE(0),
    /**
     * An external counter will be used to specify which scenario indices should be run by
     * each instance of W.I.S.E..
     */
    EXTERNAL_COUNTER(1);
    
    public final int value;
    
    LoadBalance(int value) {
        this.value = value;
    }
    
    /**
     * Convert an integer to a load balance type. The default if the integer
     * is not valid {@link LoadBalance#NONE}.
     * @param value The integer to convert to a load balance type.
     * @return The load balance type represented by the integer value, or {@link LoadBalance#NONE} if the integer is invalid.
     */
    public static LoadBalance fromInt(int value) {
        switch (value) {
        case 1:
            return EXTERNAL_COUNTER;
        }
        return NONE;
    }
    
    /**
     * Convert a string to a load balance type. The default if the string is
     * unknown is {@link LoadBalance#NONE}.
     * @param value The string value to convert to a load balance type.
     * @return The load balance type represented by the integer value, or {@link LoadBalance#NONE} if the string is unknown.
     */
    public static LoadBalance fromString(String value) {
        if (value.equalsIgnoreCase("EXTERNAL_COUNTER") || value.equals("1"))
            return EXTERNAL_COUNTER;
        return NONE;
    }
}
