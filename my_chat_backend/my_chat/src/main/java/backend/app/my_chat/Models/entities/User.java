package backend.app.my_chat.Models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;


@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_phone_number", columnList = "phone_number")
})
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(unique = true)
    private String username;

    @JsonIgnore
    private String hashedPassword;

    private String phoneNumber;

    @JsonIgnore
    private String totpSecret;

    private boolean active;

    private long createdAt;

    public User(String username, String hashedPassword, String phoneNumber, String totpSecret) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.phoneNumber = phoneNumber;
        this.totpSecret = totpSecret;
        active = false;
        createdAt = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
}
