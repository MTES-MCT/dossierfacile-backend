package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StatsRepository extends JpaRepository<Stats, Long> {
    Optional<Stats> findByKey(String key);

    @Query(value = "SELECT count(*) FROM tenant_log WHERE log_type='ACCOUNT_VALIDATED'", nativeQuery = true)
    Long countValidatedDossier();
}
