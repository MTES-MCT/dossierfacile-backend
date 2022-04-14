package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.form.SubscriptionApartmentSharingOfTenantForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.repository.PropertyApartmentSharingRepository;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class TenantServiceImpl implements TenantService {
    private final RegisterFactory registerFactory;
    private final PropertyService propertyService;
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;
    private final TenantCommonRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;

    @Override
    public <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step) {
        return registerFactory.get(step.getLabel()).saveStep(tenant, formStep);
    }

    @Override
    public void subscribeApartmentSharingOfTenantToPropertyOfOwner(String propertyToken, SubscriptionApartmentSharingOfTenantForm subscriptionApartmentSharingOfTenantForm, Tenant tenant) {
        if (tenant.getTenantType() == TenantType.CREATE) {
            Property property = propertyService.getPropertyByToken(propertyToken);
            PropertyApartmentSharing propertyApartmentSharing = propertyApartmentSharingRepository.findByPropertyAndApartmentSharing(property, tenant.getApartmentSharing()).orElse(
                    PropertyApartmentSharing.builder()
                            .accessFull(subscriptionApartmentSharingOfTenantForm.getAccess())
                            .token(subscriptionApartmentSharingOfTenantForm.getAccess() ? tenant.getApartmentSharing().getToken() : tenant.getApartmentSharing().getTokenPublic())
                            .property(property)
                            .apartmentSharing(tenant.getApartmentSharing())
                            .build()
            );
            propertyApartmentSharingRepository.save(propertyApartmentSharing);
        }
    }

    @Override
    public void updateTenantStatus(Tenant tenant) {
        TenantFileStatus previousStatus = tenant.getStatus();
        tenant.setStatus(tenant.computeStatus());
        log.info("Updating status of tenant with ID [" + tenant.getId() + "] to [" + tenant.getStatus() + "]");
        tenantRepository.save(tenant);

        if (previousStatus != tenant.getStatus()) {
            switch (tenant.getStatus()) {
                case VALIDATED: partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);
                case DECLINED: partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
            }
        } else if (previousStatus == TenantFileStatus.INCOMPLETE && tenant.getStatus() == TenantFileStatus.TO_PROCESS) {
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.CREATED_ACCOUNT);
        }
    }

    @Override
    public void updateLastLoginDateAndResetWarnings(Tenant tenant) {
        tenant.setLastLoginDate(LocalDateTime.now());
        tenant.setWarnings(0);
        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.RETURNED_ACCOUNT);
        }
        log.info("Updating last_login_date of tenant with ID [" + tenant.getId() + "]");
        tenantRepository.save(tenant);
    }
}
