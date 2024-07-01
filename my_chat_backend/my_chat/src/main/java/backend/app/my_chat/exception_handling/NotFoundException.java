package backend.app.my_chat.exception_handling;


public class NotFoundException extends Exception {
    public NotFoundException() {}

    public NotFoundException(String message) {
        super(message);
    }
}
