package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Page<Owner> findAll(Pageable pageable);

    Page<Owner> findAllByEmailContaining(String email, PageRequest page);
}
