package backend.app.my_chat.controllers;

import backend.app.my_chat.Models.dto.inputDto.UserInputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.exception_handling.BadRequestException;
import backend.app.my_chat.exception_handling.Validation;
import backend.app.my_chat.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/account/register")
    public User register(
            @RequestBody @Validated UserInputDto userInputDto,
            BindingResult validation
    ) throws BadRequestException, NoSuchAlgorithmException {
        Validation.validateBody(validation);
        return authService.register(userInputDto);
    }


}
