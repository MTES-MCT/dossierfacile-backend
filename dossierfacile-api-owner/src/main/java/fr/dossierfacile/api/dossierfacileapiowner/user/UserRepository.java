package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
