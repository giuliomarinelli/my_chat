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

    public void printKeys() {
        try {
            // Creazione del generatore di chiavi per HMAC-SHA256
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");

            // Generazione della chiave segreta di 256 bit
            SecretKey secretKey = keyGen.generateKey();

            // Convertire la chiave segreta in formato byte
            byte[] encodedKey = secretKey.getEncoded();

            // Stampa la chiave segreta (solitamente viene convertita in Base64 per l'uso con JWT)
            System.out.println("Chiave segreta in formato Base64: " + java.util.Base64.getEncoder().encodeToString(encodedKey));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(String... args) throws Exception {

        Twilio.init(accountSid, authToken);

        for (int i = 0; i < 3; i++) printKeys();

    }
}
