package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final AuthenticationFacade authenticationFacade;
    private final PropertyRepository propertyRepository;
    private final OwnerPropertyMapper propertyMapper;
    private final PropertyApartmentSharingService propertyApartmentSharingService;
    private final TenantCommonService tenantService;
    private final PropertyLogRepository propertyLogRepository;
    private final MailService mailService;

    @Qualifier("tenantJwtDecoder")
    @Autowired
    private JwtDecoder tenantJwtDecoder;

    @Override
    public PropertyModel createOrUpdate(PropertyForm propertyForm) {
        Owner owner = authenticationFacade.getOwner();
        Property property;
        if (propertyForm.getId() != null) {
            property = propertyRepository.findByIdAndOwnerId(propertyForm.getId(), owner.getId()).orElse(new Property());
        } else {
            property = new Property();
            property.setName("Propriété");
        }
        if (propertyForm.getName() != null && !propertyForm.getName().isBlank()) {
            property.setName(propertyForm.getName());
        }
        if (propertyForm.getType() != null) {
            property.setType(propertyForm.getType());
        }
        if (propertyForm.getAddress() != null) {
            property.setAddress(propertyForm.getAddress());
        }
        if (propertyForm.getFurniture() != null) {
            property.setFurniture(propertyForm.getFurniture());
        }
        if (propertyForm.getRentCost() != null && propertyForm.getRentCost() >= 0) {
            property.setRentCost(propertyForm.getRentCost());
        }
        if (propertyForm.getChargesCost() != null && propertyForm.getChargesCost() >= 0) {
            property.setChargesCost(propertyForm.getChargesCost());
        }
        if (propertyForm.getLivingSpace() != null && propertyForm.getLivingSpace() >= 0) {
            property.setLivingSpace(propertyForm.getLivingSpace());
        }
        if (propertyForm.getCo2Emission() != null && propertyForm.getCo2Emission() >= 0) {
            property.setCo2Emission(propertyForm.getCo2Emission());
        }
        if (propertyForm.getEnergyConsumption() != null && propertyForm.getEnergyConsumption() >= 0) {
            property.setEnergyConsumption(propertyForm.getEnergyConsumption());
        }
        if (propertyForm.getValidated() != null && propertyForm.getValidated()) {
            property.setValidated(true);
            property.setValidatedDate(LocalDateTime.now());
            mailService.sendEmailValidatedProperty(owner, property);
        }
        property.setOwner(owner);
        return propertyMapper.toPropertyModel(propertyRepository.save(property));
    }

    @Override
    public List<PropertyModel> getAllProperties() {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        return properties.stream().map(propertyMapper::toPropertyModel).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        Optional<Property> property = properties.stream().filter(p -> p.getId().equals(id)).findFirst();
        property.ifPresent(propertyRepository::delete);
    }

    @Override
    public Optional<Property> getProperty(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public Optional<Property> getPropertyByToken(String token) {
        return propertyRepository.findByToken(token);
    }

    @Override
    public void subscribeTenantToProperty(String propertyToken, String kcTenantToken) {
        // get tenant from jwt, then tenant give his consent
        Jwt jwt = tenantJwtDecoder.decode(kcTenantToken);
        String tenantKeycloakId = jwt.getClaimAsString("sub");
        Tenant tenant = tenantService.findByKeycloakId(tenantKeycloakId);

        Property property = getPropertyByToken(propertyToken).get();

        propertyApartmentSharingService.subscribeTenantApartmentSharingToProperty(tenant, property, true);
    }

    @Override
    public void logAccess(Property property) {
        propertyLogRepository.save(new PropertyLog(
                property
        ));
    }

}
