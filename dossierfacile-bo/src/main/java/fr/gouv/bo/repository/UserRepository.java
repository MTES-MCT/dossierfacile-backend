package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    User findOneByEmail(String email);

    @Query("SELECT distinct u from UserRole ur join ur.user u where (ur.role = 2 or ur.role=3) and u.id in :userId")
    List<User> findAllAdmins(@Param("userId") int[] userId);

    @Query("SELECT distinct u from User u left join u.userRoles ur where ur.role = 2 or ur.role=3 or ur.role=5 or (u.email like concat('%', lower(:ad)) and u.provider='google')")
    List<User> findAllAdmins(@Param("ad") String ad);
}
