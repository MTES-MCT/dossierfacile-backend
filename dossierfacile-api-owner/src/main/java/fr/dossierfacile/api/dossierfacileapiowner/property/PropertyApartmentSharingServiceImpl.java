package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PropertyApartmentSharingServiceImpl implements PropertyApartmentSharingService {
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final MailService mailService;

    @Override
    public void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing) {
        propertyApartmentSharingRepository.delete(propertyApartmentSharing);
    }

    @Override
    public void subscribeTenantApartmentSharingToProperty(Tenant tenant, Property property, boolean hasAccess) {
        if (tenant.getTenantType() == TenantType.CREATE) {
            PropertyApartmentSharing propertyApartmentSharing = propertyApartmentSharingRepository
                    .findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing())
                    .orElse(PropertyApartmentSharing.builder()
                            .accessFull(hasAccess)
                            .token(hasAccess ? tenant.getApartmentSharing().getToken() : tenant.getApartmentSharing().getTokenPublic())
                            .property(property)
                            .apartmentSharing(tenant.getApartmentSharing())
                            .build()
                    );
            propertyApartmentSharingRepository.save(propertyApartmentSharing);
            mailService.sendEmailNewApplicant(tenant, property.getOwner(), property);
        } else {
            throw new IllegalStateException("Tenant is not the main tenant");
        }
    }
}
