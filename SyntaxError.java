@SuppressWarnings("serial")
public class SyntaxError extends java.lang.Error {
    public SyntaxError(String errorMessage) {
        super(errorMessage);
    }
    public SyntaxError(String errorMessage, String lineNumber) {
        super(String.format("%s on line %d", errorMessage, lineNumber));
    }
}