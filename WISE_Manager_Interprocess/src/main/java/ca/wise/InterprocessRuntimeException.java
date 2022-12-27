package ca.wise;

public class InterprocessRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public InterprocessRuntimeException(String message) {
        super(message);
    }
    
    public InterprocessRuntimeException(Throwable e) {
        super(e);
    }
    
    public InterprocessRuntimeException(String message, Throwable e) {
        super(message, e);
    }
}
