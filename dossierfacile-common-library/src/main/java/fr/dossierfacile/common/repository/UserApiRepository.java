package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface UserApiRepository extends JpaRepository<UserApi, Long> {

    Optional<UserApi> findByName(String name);
}
