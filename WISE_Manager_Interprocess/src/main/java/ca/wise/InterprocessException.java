package ca.wise;

/**
 * An exception that is thrown by classes in the interprocess library.
 */
public class InterprocessException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public InterprocessException(String message) {
        super(message);
    }
    
    public InterprocessException(Throwable e) {
        super(e);
    }
    
    public InterprocessException(String message, Throwable e) {
        super(message, e);
    }
}
