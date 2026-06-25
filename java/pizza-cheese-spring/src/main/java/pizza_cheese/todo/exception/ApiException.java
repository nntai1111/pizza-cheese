package pizza_cheese.todo.exception;

public class ApiException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String statusMessage;

    public ApiException(int statusCode, String statusMessage, String detail) {
        this(statusCode, statusMessage, detail, null);
    }

    public ApiException(int statusCode, String statusMessage, String detail, Throwable cause) {
        super(detail, cause);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public static ApiException notFound(String detail) {
        return new ApiException(404, "Not Found", detail);
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(400, "Bad Request", detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(409, "Conflict", detail);
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(401, "Unauthorized", detail);
    }

    public static ApiException uploadFailed(String detail, Throwable cause) {
        return new ApiException(400, "Upload failed", detail, cause);
    }
}
