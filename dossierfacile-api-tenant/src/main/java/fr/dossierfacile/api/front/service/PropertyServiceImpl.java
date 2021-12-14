package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.PropertyNotFoundException;
import fr.dossierfacile.api.front.repository.PropertyRepository;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;

    @Override
    public Property getPropertyByToken(String token) {
        return propertyRepository.findFirstByToken(token).orElseThrow(() -> new PropertyNotFoundException(token));
    }
}
