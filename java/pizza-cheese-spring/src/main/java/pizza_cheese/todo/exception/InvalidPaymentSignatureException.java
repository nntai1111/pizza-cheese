package pizza_cheese.todo.exception;

public class InvalidPaymentSignatureException extends RuntimeException {

    public InvalidPaymentSignatureException(String message) {
        super(message);
    }
}
