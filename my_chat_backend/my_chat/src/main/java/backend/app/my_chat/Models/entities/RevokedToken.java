package backend.app.my_chat.Models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "revoked_tokens")
@Getter
@AllArgsConstructor
public class RevokedToken {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID jti;

}
