package fr.dossierfacile.scheduler.tasks.ownerwarning;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
interface OwnerRepository extends JpaRepository<Owner, Long> {

    @Query(value = """
            select o from Owner o
            where o.lastLoginDate < :localDateTime
            and o.warnings = :warnings
            """)
    Page<Owner> findByLastLoginDate(Pageable pageable, @Param("localDateTime") LocalDateTime localDateTime, @Param("warnings") Integer warnings);

}
