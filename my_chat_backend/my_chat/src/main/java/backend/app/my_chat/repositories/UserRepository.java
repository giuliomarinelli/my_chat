package backend.app.my_chat.repositories;

import backend.app.my_chat.Models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query(value = "SELECT * FROM users u WHERE u.username = :username AND " +
            "(u.active = true OR (EXTRACT(EPOCH FROM now()) * 1000 - u.created_at) < :timeToActivate)", nativeQuery = true)
    Optional<User> findValidUserByUsername(String username, Long timeToActivate);

    @Query(value = "SELECT * FROM users u WHERE u.id = :id AND " +
            "(u.active = true OR (EXTRACT(EPOCH FROM now()) * 1000 - u.created_at) < :timeToActivate)", nativeQuery = true)
    Optional<User> findValidUserById(UUID id, Long timeToActivate);
}
