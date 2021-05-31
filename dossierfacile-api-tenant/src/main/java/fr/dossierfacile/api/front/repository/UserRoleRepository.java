package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<List<UserRole>> findByUser(User user);
}
