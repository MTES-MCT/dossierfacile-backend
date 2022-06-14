package fr.dossierfacile.migrate.user.repository;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<User, Long> {

    Optional<Owner> findByEmail(String email);

}
