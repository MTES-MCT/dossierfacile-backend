package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findOneByEmailAndEnabledTrue(String toLowerCase);

    Optional<User> findByEmail(String email);

}
