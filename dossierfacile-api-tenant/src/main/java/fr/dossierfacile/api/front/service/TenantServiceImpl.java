package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.PropertyNotFoundException;
import fr.dossierfacile.api.front.form.SubscriptionTenantForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.repository.PropertyApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.PropertyRepository;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TenantServiceImpl implements TenantService {
    private final RegisterFactory registerFactory;
    private final PropertyRepository propertyRepository;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;

    @Override
    public <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step) {
        return registerFactory.get(step.getLabel()).saveStep(tenant, formStep);
    }

    @Override
    public void subscribeTenant(String propertyToken, SubscriptionTenantForm subscriptionTenantForm, Tenant tenant) {
        if(tenant.getTenantType() == TenantType.CREATE) {
            Property property = propertyRepository.findFirstByToken(propertyToken).orElseThrow(() -> new PropertyNotFoundException(propertyToken));
            PropertyApartmentSharing propertyApartmentSharing = propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()).orElse(
                    PropertyApartmentSharing.builder()
                            .accessFull(subscriptionTenantForm.getAccess())
                            .token(subscriptionTenantForm.getAccess() ? tenant.getApartmentSharing().getToken() : tenant.getApartmentSharing().getTokenPublic())
                            .property(property)
                            .apartmentSharing(tenant.getApartmentSharing())
                            .build()
            );
            propertyApartmentSharingRepository.save(propertyApartmentSharing);
        }
    }
}
