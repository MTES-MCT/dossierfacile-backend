package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserApiRepository extends JpaRepository<UserApi, Long> {

    Optional<UserApi> findByName(String name);
}
