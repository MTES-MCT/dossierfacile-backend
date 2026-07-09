package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.exception.ApplicationRegistrationException;
import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Business rules of the application step that cannot live in the bean-validation
 * annotations of {@link ApplicationFormV2}: they depend on the logged tenant or
 * on database lookups. Static/structural rules stay on the form itself so they
 * are enforced by Spring on every {@code @Validated} binding.
 */
@Component
@RequiredArgsConstructor
public class ApplicationRegistrationValidator {

    private final TenantCommonRepository tenantRepository;

    /**
     * Throws an {@link ApplicationRegistrationException} carrying the matching
     * {@link ApplicationErrorCode} on the first violation.
     */
    public void validate(Tenant tenant, ApplicationFormV2 applicationForm) {
        if (tenant.getTenantType() == TenantType.JOIN) {
            throw new ApplicationRegistrationException(ApplicationErrorCode.APPLICATION_TYPE_DENIED_FOR_JOIN);
        }

        List<CoTenantForm> coTenants = applicationForm.getCoTenants() == null
                ? Collections.emptyList()
                : applicationForm.getCoTenants();

        if (hasEmailConflict(tenant, coTenants)) {
            throw new ApplicationRegistrationException(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * A submitted co-tenant email conflicts when it already belongs to an account,
     * unless it is the email of a co-tenant already part of this dossier.
     */
    private boolean hasEmailConflict(Tenant tenant, List<CoTenantForm> coTenants) {
        return coTenants.stream()
                .filter(form -> StringUtils.isNotBlank(form.getEmail()))
                .filter(form -> !matchesExistingCoTenant(form, tenant))
                .anyMatch(form -> tenantRepository.existsByEmail(form.getEmail().toLowerCase()));
    }

    // Matching on email alone: the frontend never lets a co-tenant email change once set,
    // so an email found in the dossier always designates the same person (even if names
    // were completed or fixed since). The logged tenant is excluded so submitting one's
    // own email as co-tenant email is still a conflict.
    private boolean matchesExistingCoTenant(CoTenantForm form, Tenant tenant) {
        return tenant.getApartmentSharing().getTenants().stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .anyMatch(t -> form.getEmail().equalsIgnoreCase(t.getEmail()));
    }
}
