package backend.app.my_chat.repositories;

import backend.app.my_chat.Models.entities.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
}
