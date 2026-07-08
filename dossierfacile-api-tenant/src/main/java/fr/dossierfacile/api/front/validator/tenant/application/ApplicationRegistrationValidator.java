package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApplicationRegistrationValidator {

    private final TenantCommonRepository tenantRepository;

    public boolean hasValidStructure(ApplicationFormV2 applicationForm) {
        if (applicationForm.getApplicationType() == null) {
            return false;
        }
        return switch (applicationForm.getApplicationType()) {
            case COUPLE -> applicationForm.getCoTenants().size() == 1 && hasDistinctEmails(applicationForm.getCoTenants());
            case GROUP -> !applicationForm.getCoTenants().isEmpty() && hasDistinctEmails(applicationForm.getCoTenants());
            case ALONE -> applicationForm.getCoTenants().isEmpty();
        };
    }

    public Optional<ApplicationErrorCode> validate(Tenant tenant, ApplicationFormV2 applicationForm) {
        if (tenant.getTenantType() == TenantType.JOIN) {
            return Optional.of(ApplicationErrorCode.APPLICATION_TYPE_DENIED_FOR_JOIN);
        }

        ApplicationType applicationType = applicationForm.getApplicationType();
        if ((applicationType == ApplicationType.GROUP || applicationType == ApplicationType.COUPLE)
                && (applicationForm.getAcceptAccess() == null || !applicationForm.getAcceptAccess())) {
            return Optional.of(ApplicationErrorCode.ACCEPT_ACCESS_REQUIRED);
        }

        if ((applicationType == ApplicationType.GROUP || applicationType == ApplicationType.COUPLE)
                && !CollectionUtils.isEmpty(applicationForm.getCoTenants())
                && applicationForm.getCoTenants().stream().anyMatch(t -> StringUtils.isBlank(t.getEmail()))) {
            return Optional.of(ApplicationErrorCode.CO_TENANT_EMAIL_REQUIRED);
        }

        if (hasEmailConflict(tenant, applicationForm)) {
            return Optional.of(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
        }

        return Optional.empty();
    }

    /**
     * A submitted co-tenant email conflicts when it already belongs to an account,
     * unless it is the unchanged email of a co-tenant already part of this dossier.
     */
    private boolean hasEmailConflict(Tenant tenant, ApplicationFormV2 applicationForm) {
        return applicationForm.getCoTenants().stream()
                .filter(form -> StringUtils.isNotBlank(form.getEmail()))
                .filter(form -> !matchesExistingCoTenant(form, tenant))
                .anyMatch(form -> tenantRepository.existsByEmail(form.getEmail().toLowerCase()));
    }

    // Same names AND same email: a renamed co-tenant is treated as a new person (parity with legacy behavior)
    private boolean matchesExistingCoTenant(CoTenantForm form, Tenant tenant) {
        return tenant.getApartmentSharing().getTenants().stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .anyMatch(t -> Objects.equals(t.getFirstName(), form.getFirstName())
                        && Objects.equals(t.getLastName(), form.getLastName())
                        && form.getEmail().equalsIgnoreCase(t.getEmail()));
    }

    private static boolean hasDistinctEmails(List<CoTenantForm> coTenantForms) {
        var emails = coTenantForms.stream()
                .filter(t -> StringUtils.isNotBlank(t.getEmail()))
                .toList();
        return emails.stream().map(CoTenantForm::getEmail).distinct().count() == emails.size();
    }
}
