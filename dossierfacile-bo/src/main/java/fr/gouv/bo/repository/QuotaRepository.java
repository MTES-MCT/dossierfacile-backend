package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Quota;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

public interface QuotaRepository extends CrudRepository<Quota, Long> {
    Set<Quota> findAllByEmail(String email);
}
