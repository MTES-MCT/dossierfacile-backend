package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findOneByEmailAndEnabledTrue(String toLowerCase);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
