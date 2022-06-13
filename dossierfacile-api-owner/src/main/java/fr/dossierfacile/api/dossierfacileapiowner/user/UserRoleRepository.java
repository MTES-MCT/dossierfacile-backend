package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);
}
