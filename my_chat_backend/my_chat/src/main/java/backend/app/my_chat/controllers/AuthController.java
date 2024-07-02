package backend.app.my_chat.controllers;

import backend.app.my_chat.Models.TokenPair;
import backend.app.my_chat.Models.dto.inputDto.LoginInputDto;
import backend.app.my_chat.Models.dto.inputDto.OtpInputDto;
import backend.app.my_chat.Models.dto.inputDto.UserInputDto;
import backend.app.my_chat.Models.dto.outputDto.ConfirmOutputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.exception_handling.BadRequestException;
import backend.app.my_chat.exception_handling.NotFoundException;
import backend.app.my_chat.exception_handling.UnauthorizedException;
import backend.app.my_chat.exception_handling.Validation;
import backend.app.my_chat.repositories.UserRepository;
import backend.app.my_chat.security.JwtUtils;
import backend.app.my_chat.services.AuthService;
import backend.app.my_chat.services.TwilioSmsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

    @Autowired
    private TwilioSmsService twilioSmsService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/account/register")
    public User register(
            @RequestBody @Validated UserInputDto userInputDto,
            BindingResult validation
    ) throws BadRequestException, NoSuchAlgorithmException {
        Validation.validateBody(validation);
        return authService.register(userInputDto);
    }

    @GetMapping("/account/{userId}/send-otp")
    public ConfirmOutputDto sendOtp(@PathVariable UUID userId, @RequestParam("a") String action, HttpServletRequest req) throws NotFoundException, NoSuchAlgorithmException, InvalidKeyException, BadRequestException, UnauthorizedException {

        String messageBody = "";

        Cookie[] cookies = req.getCookies();

        System.out.println(Arrays.stream(cookies).toList());

        User user = userRepository.findValidUserById(userId, timeToActivate).orElseThrow(
                () -> new NotFoundException("Utente con id='" + userId + "' non trovato")
        );

        switch (action) {
            case "activation" -> {
                messageBody = "Il codice di verifica per attivare il tuo account my_chat è ";
                if (user.isActive())
                    throw new BadRequestException("L'utente con id='" + userId + "' è già stato attivato");
            }
            case "auth" -> {
                messageBody = "Il codice di verifica per accedere a my_chat è ";
                if (cookies.length == 0) throw new UnauthorizedException("Pre Authorization Token non fornito");
                if (Arrays.stream(cookies).noneMatch(c -> c.getName().equals("__pre_authorization_tkn")))
                    throw new UnauthorizedException("Pre Authorization Token non fornito");
                Cookie preAuthorizationToken = Arrays.stream(cookies)
                        .filter(c -> c.getName().equals("__pre_authorization_tkn")).findFirst().get();
                UUID _userId = jwtUtils.verifyPreAuthorizationTokenAndExtractUserId(preAuthorizationToken.getValue());
                if (!userId.equals(_userId))
                    throw new UnauthorizedException("userId fornito non valido");

            }
            default -> throw new BadRequestException("Url malformato. Il query param 'a' " +
                    "deve assumere i valori 'activation' o 'auth'");
        }


        String otp = authService.generateTotp(user.getTotpSecret(), 6, 120);

        twilioSmsService.sendSms(user.getPhoneNumber(), messageBody + otp);

        return new ConfirmOutputDto(HttpStatus.OK, "Un sms con un codice di verifica è stato inviato" +
                " al numero " + authService.obscurePhoneNumber(user.getPhoneNumber()));

    }

    @PostMapping("/account/{userId}/activate")
    public User activateAccount(@RequestBody @Validated OtpInputDto otpInputDto, BindingResult validation, @PathVariable UUID userId) throws UnauthorizedException, NotFoundException, NoSuchAlgorithmException, InvalidKeyException, BadRequestException {
        Validation.validateBody(validation);
        return authService.activateAccount(otpInputDto.otp(), userId);
    }

    @PostMapping("/account/login/1")
    public ConfirmOutputDto login(@RequestBody @Validated LoginInputDto loginInputDto, BindingResult validation, HttpServletResponse res) throws UnauthorizedException {
        Cookie preAuthorizationToken = new Cookie("__pre_authorization_tkn", authService.loginStep1(loginInputDto));
        preAuthorizationToken.setPath("/");
        preAuthorizationToken.setDomain("localhost");
        preAuthorizationToken.setHttpOnly(true);
        res.addCookie(preAuthorizationToken);
        return new ConfirmOutputDto(HttpStatus.OK, "Autenticazione effettuata con successo. Ora è necessariaù" +
                " l'autenticazione a due fattori");
    }

    @PostMapping("/account/login/2")
    public ConfirmOutputDto login(@RequestBody @Validated OtpInputDto otpInputDto, BindingResult validation, HttpServletResponse res, HttpServletRequest req) throws UnauthorizedException, NoSuchAlgorithmException, InvalidKeyException {

        Cookie[] cookies = req.getCookies();

        if (cookies.length == 0) throw new UnauthorizedException("Pre Authorization Token non fornito");
        if (Arrays.stream(cookies)
                .noneMatch(c -> c.getName().equals("__pre_authorization_tkn"))) {
            throw new UnauthorizedException("Pre Authorization Token non fornito");
        }

        Cookie preAuthorizationToken = Arrays.stream(cookies)
                .filter(c -> c.getName().equals("__pre_authorization_tkn")).findFirst().get();
        UUID userId = jwtUtils.verifyPreAuthorizationTokenAndExtractUserId(preAuthorizationToken.getValue());

        User user = userRepository.findValidUserById(userId, timeToActivate).orElseThrow(
                () -> new UnauthorizedException("Credenziali di accesso non valide. E' possibile " +
                        "che sia scaduto il tempo per inserire il codice a 6 cifre")
        );

        TokenPair tokenPair = authService.loginStep2(otpInputDto.otp(), user);

        Cookie accessToken = new Cookie("__access_tkn", tokenPair.getAccessToken());
        accessToken.setPath("/");
        accessToken.setDomain("localhost");
        accessToken.setHttpOnly(true);
        res.addCookie(accessToken);

        Cookie refreshToken = new Cookie("__refresh_tkn", tokenPair.getRefreshToken());
        refreshToken.setPath("/");
        refreshToken.setDomain("localhost");
        refreshToken.setHttpOnly(true);
        res.addCookie(refreshToken);

        Cookie _preAuthorizationToken = new Cookie("__pre_authorization_tkn", null);
        _preAuthorizationToken.setMaxAge(0);
        _preAuthorizationToken.setPath("/");
        res.addCookie(_preAuthorizationToken);

        return new ConfirmOutputDto(HttpStatus.OK, "Login completato con successo");

    }

    @GetMapping("/account/logout")
    public ConfirmOutputDto logout(HttpServletResponse res) {
        Cookie accessToken = new Cookie("__access_tkn", null);
        accessToken.setMaxAge(0);
        accessToken.setPath("/");
        Cookie refreshToken = new Cookie("__refresh_tkn", null);
        refreshToken.setMaxAge(0);
        refreshToken.setPath("/");
        res.addCookie(accessToken);
        res.addCookie(refreshToken);
        return new ConfirmOutputDto(HttpStatus.OK, "Logout effettuato con successo");
    }
}
