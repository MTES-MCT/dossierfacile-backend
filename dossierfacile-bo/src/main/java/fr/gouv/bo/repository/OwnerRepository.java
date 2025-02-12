package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    @Query("""
            SELECT o FROM Owner o
            WHERE LOWER(o.email) LIKE %:email%
            AND LOWER(o.firstName) LIKE %:firstName%
            AND LOWER(o.lastName) LIKE %:lastName%
            """)
    Page<Owner> searchOwners(
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            PageRequest page);
}
