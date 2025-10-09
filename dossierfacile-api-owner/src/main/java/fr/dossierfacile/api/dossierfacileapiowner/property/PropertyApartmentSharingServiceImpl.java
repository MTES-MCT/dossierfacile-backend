package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static fr.dossierfacile.common.entity.PropertyLog.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PropertyApartmentSharingServiceImpl implements PropertyApartmentSharingService {
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final PropertyLogRepository logRepository;
    private final MailService mailService;

    @Override
    public void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing) {
        ApartmentSharing apartmentSharing = propertyApartmentSharing.getApartmentSharing();
        Long id = propertyApartmentSharing.getProperty().getId();
        Optional<ApartmentSharingLink> link = apartmentSharing
            .getApartmentSharingLinks()
            .stream()
            .filter(l -> id.equals(l.getPropertyId()))
            .findFirst();
        if (link.isPresent()) {
            apartmentSharingLinkRepository.delete(link.get());
        }
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
                                .property(property)
                                .apartmentSharing(apartmentSharing)
                                .build();
                propertyApartmentSharingRepository.save(propertyApartmentSharing);
                logRepository.save(applicationReceived(property, apartmentSharing));
                ApartmentSharingLink apartmentSharingLink = ApartmentSharingLink.builder()
                    .apartmentSharing(apartmentSharing)
                    .property(property)
                    .token(UUID.randomUUID())
                    .creationDate(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusMonths(1))
                    .fullData(hasAccess)
                    .linkType(ApartmentSharingLinkType.OWNER)
                    .title(property.getAddress())
                    .createdBy(tenant.getId())
                    .build();
                apartmentSharingLinkRepository.save(apartmentSharingLink);
                if (apartmentSharing.getStatus() == TenantFileStatus.VALIDATED) {
                    mailService.sendEmailNewApplicantValidated(tenant, property.getOwner(), property);
                } else {
                    mailService.sendEmailNewApplicantNotValidated(tenant, property.getOwner(), property);
                }
            }
        } else {
            throw new IllegalStateException("Tenant is not the main tenant");
        }
    }

}
