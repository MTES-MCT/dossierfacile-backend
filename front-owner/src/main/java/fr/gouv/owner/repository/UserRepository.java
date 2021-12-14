package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    void deleteUserTenantSharingApartment(@Param("userId") Long userId);

    User findOneByEmail(String email);

    @Query("SELECT distinct u from UserRole ur join ur.user u where (ur.role = 2 or ur.role=3) and u.id in :userId")
    List<User> findAllAdmins(@Param("userId") int[] userId);

    Optional<User> findByEmail(String email);
}
