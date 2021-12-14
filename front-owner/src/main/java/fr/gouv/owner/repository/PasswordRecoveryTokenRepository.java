package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.repository.CrudRepository;


public interface PasswordRecoveryTokenRepository extends CrudRepository<PasswordRecoveryToken, Integer> {

    PasswordRecoveryToken findOneByToken(String token);

    PasswordRecoveryToken findOneByUser(User user);
}
