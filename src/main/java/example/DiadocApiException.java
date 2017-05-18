package example;

public class DiadocApiException extends RuntimeException {
    public DiadocApiException() {
    }

    public DiadocApiException(String message) {
        super(message);
    }

    public DiadocApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiadocApiException(Throwable cause) {
        super(cause);
    }
}
