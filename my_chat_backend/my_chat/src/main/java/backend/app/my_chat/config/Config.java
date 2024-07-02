package backend.app.my_chat.config;

import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@PropertySource("application.properties")
public class Config {

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean("time_to_activate")
    public Long getTimeToActivateAccount(@Value("${user.timeToActivate}") String timeToActivate) {
        System.out.println(timeToActivate);
        return Long.parseLong(timeToActivate);
    }

}
