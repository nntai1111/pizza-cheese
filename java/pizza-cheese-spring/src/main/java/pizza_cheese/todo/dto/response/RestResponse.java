package pizza_cheese.todo.dto.response;

public class RestResponse<T> {

    private int statusCode;
    private String message;
    private String error;
    private T data;

    public RestResponse() {
    }

    public RestResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public static <T> RestResponse<T> success(T data) {
        RestResponse<T> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setMessage("Success");
        response.setData(data);
        return response;
    }

    public static <T> RestResponse<T> error(int statusCode, String message, String error) {
        RestResponse<T> response = new RestResponse<>();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setError(error);
        return response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
