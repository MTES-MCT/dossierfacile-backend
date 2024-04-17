package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.BOUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BOUserRepository extends JpaRepository<BOUser, Long> {
    Optional<BOUser> findByEmail(String email);

    @Query("SELECT distinct u from User u left join u.userRoles ur where ur.role = 2 or ur.role=3 or ur.role=5 or (u.email like concat('%', lower(:ad)) and u.provider='google')")
    List<BOUser> findAllAdmins(@Param("ad") String ad);

    Optional<BOUser> findById(Long id);
}
