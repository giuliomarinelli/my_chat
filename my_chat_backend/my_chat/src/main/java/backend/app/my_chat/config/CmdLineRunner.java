package backend.app.my_chat.config;

import com.twilio.Twilio;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

@Configuration
@Component
public class CmdLineRunner implements CommandLineRunner {

    @Value("${twilio.accountSid}")
    private String accountSid;
    @Value("${twilio.authToken}")
    private String authToken;



    @Override
    public void run(String... args) throws Exception {

        Twilio.init(accountSid, authToken);


    }
}
