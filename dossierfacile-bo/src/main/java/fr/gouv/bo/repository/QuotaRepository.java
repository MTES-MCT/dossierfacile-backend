package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Quota;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuotaRepository extends CrudRepository<Quota, Long> {
    List<Quota> findAllByEmail(String email);
}
