package pizza_cheese.todo.exception;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
