@SuppressWarnings("serial")
public class NoPythonFoundError extends RuntimeException {
    public NoPythonFoundError(String errorMessage) {
        super(errorMessage);
    }
}