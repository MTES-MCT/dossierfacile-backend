package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

public interface UserRoleRepository extends CrudRepository<UserRole, Integer> {
    Set<UserRole> findAllByUser(User user);
}
