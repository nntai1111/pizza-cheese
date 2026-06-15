package pizza_cheese.todo.exception;

public class ToppingNotFoundException extends RuntimeException {

    public ToppingNotFoundException(String message) {
        super(message);
    }
}
