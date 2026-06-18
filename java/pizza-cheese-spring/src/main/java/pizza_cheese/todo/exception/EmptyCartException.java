package pizza_cheese.todo.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException(String message) {
        super(message);
    }
}
