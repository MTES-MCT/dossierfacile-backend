package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<User, Long> {
}
