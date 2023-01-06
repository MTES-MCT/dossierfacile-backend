package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.SubscriptionApartmentSharingOfTenantForm;
import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface TenantService {
    <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step);

    void subscribeApartmentSharingOfTenantToPropertyOfOwner(String propertyToken, SubscriptionApartmentSharingOfTenantForm subscriptionApartmentSharingOfTenantForm, Tenant tenant);

    Tenant updateTenantStatus(Tenant tenant);

    void updateLastLoginDateAndResetWarnings(Tenant tenant);

    Tenant create(Tenant tenant);

    EmailExistsModel emailExists(EmailExistsForm emailExistsForm);

    Tenant findById(Long coTenantId);

    Tenant findByKeycloakId(String keycloakId);

    @Transactional
    void processWarningsBatch(LocalDateTime localDateTime, int warnings, Pageable page);
}
