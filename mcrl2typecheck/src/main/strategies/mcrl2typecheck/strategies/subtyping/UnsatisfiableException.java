package mcrl2typecheck.strategies.subtyping;

public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsatisfiableException() {
        super();
    }

    public UnsatisfiableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsatisfiableException(String message) {
        super(message);
    }

    public UnsatisfiableException(Throwable cause) {
        super(cause);
    }

}