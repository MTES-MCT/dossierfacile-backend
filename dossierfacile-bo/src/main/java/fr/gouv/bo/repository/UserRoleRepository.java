package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    Set<UserRole> findAllByUser(User user);
}
