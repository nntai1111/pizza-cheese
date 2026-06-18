package pizza_cheese.todo.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
