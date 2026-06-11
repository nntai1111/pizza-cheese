package vn.hoidanit.todo.handler;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.hoidanit.todo.dto.response.RestResponse;
import vn.hoidanit.todo.exception.InvalidRefreshTokenException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(RestResponse.error(400, "Validation failed", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error(401, "Unauthorized", "Email hoặc mật khẩu không đúng"));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResponse.error(500, "Internal Server Error", ex.getMessage()));
    }
}
