package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Owner;
import fr.gouv.bo.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {
    private final OwnerRepository ownerRepository;

    public Page<Owner> findAllPageable(PageRequest page) {
        return ownerRepository.findAll(page);
    }

    public Page<Owner> findAllByEmailExpressionPageable(String email, PageRequest page) {
        return ownerRepository.findAllByEmailContaining(email, page);
    }
}