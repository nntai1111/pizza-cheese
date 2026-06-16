package pizza_cheese.todo.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
