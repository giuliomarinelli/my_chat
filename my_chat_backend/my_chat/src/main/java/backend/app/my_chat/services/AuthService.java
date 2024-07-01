package backend.app.my_chat.services;

import backend.app.my_chat.Models.dto.inputDto.UserInputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.exception_handling.BadRequestException;
import backend.app.my_chat.repositories.UserRepository;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    @Qualifier("time_to_activate")
    private Long timeToActivate;

    private String generateBase32Secret() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256); // Dimensione della chiave
        SecretKey secretKey = keyGenerator.generateKey();
        Base32 base32 = new Base32();
        return base32.encodeToString(secretKey.getEncoded());
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

}
