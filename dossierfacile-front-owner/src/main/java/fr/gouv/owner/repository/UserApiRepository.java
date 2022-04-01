package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserApiRepository extends JpaRepository<UserApi, Long> {

    UserApi findOneByName(String source);

    List<UserApi> findAllByTypeUserApi(TypeUserApi light);

    UserApi findOneById(Long id);
}
