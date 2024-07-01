package backend.app.my_chat.controllers;

import backend.app.my_chat.Models.dto.inputDto.UserInputDto;
import backend.app.my_chat.Models.dto.outputDto.ConfirmOutputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.exception_handling.BadRequestException;
import backend.app.my_chat.exception_handling.NotFoundException;
import backend.app.my_chat.exception_handling.Validation;
import backend.app.my_chat.repositories.UserRepository;
import backend.app.my_chat.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("time_to_activate")
    private Long timeToActivate;

    @PostMapping("/account/register")
    public User register(
            @RequestBody @Validated UserInputDto userInputDto,
            BindingResult validation
    ) throws BadRequestException, NoSuchAlgorithmException {
        Validation.validateBody(validation);
        return authService.register(userInputDto);
    }

    @GetMapping("/account/{userId}/send-otp")
    public ConfirmOutputDto sendOtp(@PathVariable UUID userId) throws NotFoundException, NoSuchAlgorithmException, InvalidKeyException {
        User user = userRepository.findValidUserById(userId, timeToActivate).orElseThrow(
                () -> new NotFoundException("Utente con id='" + userId + "' non trovato")
        );

        String otp = authService.generateTotp(user.getTotpSecret(), 6, 120);

        System.out.println(otp);

        return new ConfirmOutputDto(HttpStatus.OK, "Un sms con un codice di verifica è stato inviato");

    }

//    @PostMapping("/account/{userId}/activate")



}
