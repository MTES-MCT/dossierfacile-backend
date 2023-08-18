package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Property;
import fr.gouv.bo.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public Optional<Property> findById(Long id) {
        return propertyRepository.findById(id);
    }
}