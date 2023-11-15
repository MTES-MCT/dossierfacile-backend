package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static fr.dossierfacile.common.entity.PropertyLog.*;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PropertyApartmentSharingServiceImpl implements PropertyApartmentSharingService {
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final PropertyLogRepository logRepository;
    private final MailService mailService;

    @Override
    public void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing) {
        propertyApartmentSharingRepository.delete(propertyApartmentSharing);
        logRepository.save(applicationDeletedByOwner(propertyApartmentSharing));
    }

    @Override
    public void subscribeTenantApartmentSharingToProperty(Tenant tenant, Property property, boolean hasAccess) {
        if (tenant.getTenantType() == TenantType.CREATE) {
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Optional<PropertyApartmentSharing> existingPropertyApartmentSharing = propertyApartmentSharingRepository
                    .findByPropertyAndApartmentSharing(property, apartmentSharing);
            if (existingPropertyApartmentSharing.isEmpty()) {
                PropertyApartmentSharing propertyApartmentSharing = PropertyApartmentSharing.builder()
                                .accessFull(hasAccess)
                                .token(hasAccess ? apartmentSharing.getToken() : apartmentSharing.getTokenPublic())
                                .property(property)
                                .apartmentSharing(apartmentSharing)
                                .build();
                propertyApartmentSharingRepository.save(propertyApartmentSharing);
                logRepository.save(applicationReceived(property, apartmentSharing));
                mailService.sendEmailNewApplicant(tenant, property.getOwner(), property);
            }
        } else {
            throw new IllegalStateException("Tenant is not the main tenant");
        }
    }
}
