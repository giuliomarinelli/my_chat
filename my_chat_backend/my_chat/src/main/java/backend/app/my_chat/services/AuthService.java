package backend.app.my_chat.services;

import backend.app.my_chat.Models.TokenPair;
import backend.app.my_chat.Models.dto.inputDto.LoginInputDto;
import backend.app.my_chat.Models.dto.inputDto.UserInputDto;
import backend.app.my_chat.Models.dto.outputDto.ConfirmOutputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.Models.enums.TokenType;
import backend.app.my_chat.exception_handling.BadRequestException;
import backend.app.my_chat.exception_handling.NotFoundException;
import backend.app.my_chat.exception_handling.UnauthorizedException;
import backend.app.my_chat.repositories.UserRepository;
import backend.app.my_chat.security.JwtUtils;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    @Qualifier("time_to_activate")
    private Long timeToActivate;

    private String generateBase32Secret() throws NoSuchAlgorithmException {
        byte[] buffer = new byte[16];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);

        Base32 base32 = new Base32();
        return base32.encodeToString(buffer);
    }

    public String generateTotp(String base32Secret, int digits, int timeStepSeconds) throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(base32Secret);

        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(secretBytes, "RAW");

        TimeBasedOneTimePasswordGenerator totpGenerator =
                new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(timeStepSeconds), digits);

        Instant now = Instant.now();
        int totp = totpGenerator.generateOneTimePassword(secretKey, now);
        return String.valueOf(totp);
    }

    public boolean validateTotp(String base32Secret, int expectedTotp, int digits, int timeStepSeconds) throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(base32Secret);

        // Crea una chiave segreta da secretBytes
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "RAW");

        // Crea un generatore TOTP con parametri personalizzati
        TimeBasedOneTimePasswordGenerator totpGenerator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(timeStepSeconds), digits);

        // Calcola il TOTP per l'istante attuale
        Instant now = Instant.now();
        return totpGenerator.generateOneTimePassword(keySpec, now) == expectedTotp;
    }

    public User register(UserInputDto userInputDto) throws NoSuchAlgorithmException, BadRequestException {

        if (userRepository.findValidUserByUsername(userInputDto.username(), timeToActivate).isPresent())
            throw new BadRequestException("L'utente con username '" + userInputDto.username() + "' " +
                    "è già registrato");

        User user = new User(
                userInputDto.username(),
                encoder.encode(userInputDto.password()),
                "+39" + userInputDto.phoneNumber(),
                generateBase32Secret()
        );

        return userRepository.save(user);


    }

    public String obscurePhoneNumber(String phoneNumber) {
        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 2);
        String middle = phoneNumber.substring(3, phoneNumber.length() - 2);
        String obscuredMiddle = "*".repeat(middle.length());
        return prefix + obscuredMiddle + suffix;
    }

    public User activateAccount(String otp, UUID userId) throws NoSuchAlgorithmException, InvalidKeyException, NotFoundException, UnauthorizedException {
        User user = userRepository.findValidInactiveUserById(userId, timeToActivate).orElseThrow(
                () -> new NotFoundException("Utente con id='" + userId + "' da attivare non trovato. " +
                        "Assicurati che l'utente non sia già attivo o che non sia scaduto il tempo per l'attivazione")
        );
        boolean isValid = validateTotp(user.getTotpSecret(), Integer.parseInt(otp), 6, 120);

        if (!isValid) throw new UnauthorizedException("Il codice OTP inserito per attivare l'account non è valido");

        user.setActive(true);

        return userRepository.save(user);

    }

    public String loginStep1(LoginInputDto loginInputDto) throws UnauthorizedException {
        User user = userRepository.findValidUserByUsername(loginInputDto.username(), timeToActivate).orElseThrow(
                () -> new UnauthorizedException("Username o password errati")
        );

        if (!encoder.matches(loginInputDto.password(), user.getHashedPassword()))
            throw new UnauthorizedException("Username o password errati");

        return jwtUtils.generateToken(user, TokenType.PRE_AUTHORIZATION_TOKEN, false);

    }

    public TokenPair loginStep2(String otp, User user) throws NoSuchAlgorithmException, InvalidKeyException, UnauthorizedException {

        boolean isValid = validateTotp(user.getTotpSecret(), Integer.parseInt(otp), 6, 120);

        if (!isValid) throw new UnauthorizedException("Il codice a 6 cifre per l'autenticazione a 2 fattori non è valido");

        return new TokenPair(
                jwtUtils.generateToken(user, TokenType.ACCESS_TOKEN, false),
                jwtUtils.generateToken(user, TokenType.REFRESH_TOKEN, false)
                );

    }


}
