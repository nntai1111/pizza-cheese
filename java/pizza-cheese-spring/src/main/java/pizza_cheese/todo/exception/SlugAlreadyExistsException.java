package pizza_cheese.todo.exception;

public class SlugAlreadyExistsException extends RuntimeException {

    public SlugAlreadyExistsException(String message) {
        super(message);
    }
}
