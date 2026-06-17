package pizza_cheese.todo.handler;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.exception.CartItemNotFoundException;
import pizza_cheese.todo.exception.CategoryNotFoundException;
import pizza_cheese.todo.exception.ComboNotFoundException;
import pizza_cheese.todo.exception.EmailAlreadyExistsException;
import pizza_cheese.todo.exception.FileUploadException;
import pizza_cheese.todo.exception.InvalidRefreshTokenException;
import pizza_cheese.todo.exception.PizzaNotFoundException;
import pizza_cheese.todo.exception.SlugAlreadyExistsException;
import pizza_cheese.todo.exception.ToppingNotFoundException;
import pizza_cheese.todo.exception.UsernameAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<RestResponse<Void>> handleValidation(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException validationEx) {
            message = validationEx.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else {
            message = ((BindException) ex).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }

        return ResponseEntity.badRequest()
                .body(RestResponse.error(400, "Validation failed", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error(401, "Unauthorized", "Email/tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<RestResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<RestResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RestResponse.error(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<RestResponse<Void>> handleFileUpload(FileUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error(400, "Upload failed", ex.getMessage()));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<RestResponse<Void>> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RestResponse.error(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(PizzaNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handlePizzaNotFound(PizzaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleCartItemNotFound(CartItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ComboNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleComboNotFound(ComboNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ToppingNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleToppingNotFound(ToppingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<RestResponse<Void>> handleSlugAlreadyExists(SlugAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RestResponse.error(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(RestResponse.error(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResponse.error(500, "Internal Server Error", ex.getMessage()));
    }
}
