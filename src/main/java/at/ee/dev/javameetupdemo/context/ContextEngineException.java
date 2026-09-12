package at.ee.dev.javameetupdemo.context;

public class ContextEngineException extends RuntimeException {

    public ContextEngineException(String message) {
        super(message);
    }

    public ContextEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
