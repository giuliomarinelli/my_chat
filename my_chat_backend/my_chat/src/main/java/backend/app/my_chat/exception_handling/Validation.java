package backend.app.my_chat.exception_handling;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.stream.Collectors;

public class Validation {

    private static String getMessages(BindingResult validation) {
        return validation.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(". "));
    }

    public static void validateBody(BindingResult validation) throws BadRequestException {
        if (validation.hasErrors()) throw new BadRequestException(getMessages(validation));
    }

}
