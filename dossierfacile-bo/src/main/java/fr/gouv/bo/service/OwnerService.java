package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Owner;
import fr.gouv.bo.repository.OwnerRepository;
import fr.gouv.bo.repository.specification.OwnerSpecifications;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {
    private final OwnerRepository ownerRepository;

    public Page<Owner> searchOwners(@NotNull String email, @NotNull String firstName, @NotNull String lastName, PageRequest page) {
        Specification<Owner> specification = Specification.unrestricted();
        specification = specification.and(OwnerSpecifications.emailContains(email));
        specification = specification.and(OwnerSpecifications.firstNameContains(firstName));
        specification = specification.and(OwnerSpecifications.lastNameContains(lastName));

        return ownerRepository.findAll(specification, page);
    }

    public Optional<Owner> findById(Long id) {
        return ownerRepository.findById(id);
    }
}