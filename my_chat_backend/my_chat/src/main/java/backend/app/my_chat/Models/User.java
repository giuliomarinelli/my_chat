package backend.app.my_chat.Models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username")
})
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(unique = true)
    private String username;

    private String hashedPassword;

    private String phoneNumber;

    private String totpSecret;

    private boolean active;

    private long createdAt;


}
