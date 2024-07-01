package backend.app.my_chat.exception_handling;


public class BadRequestException extends Exception {
    public BadRequestException() {}

    public BadRequestException(String message) {
        super(message);
    }
}
