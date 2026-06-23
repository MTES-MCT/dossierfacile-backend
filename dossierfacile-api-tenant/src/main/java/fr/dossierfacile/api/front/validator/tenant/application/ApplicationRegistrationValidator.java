package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.ApartmentSharing;
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
import java.util.stream.Collectors;

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
        if (applicationType == ApplicationType.GROUP || applicationType == ApplicationType.COUPLE) {
            if (applicationForm.getAcceptAccess() == null || !applicationForm.getAcceptAccess()) {
                return Optional.of(ApplicationErrorCode.ACCEPT_ACCESS_REQUIRED);
            }
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

    private boolean hasEmailConflict(Tenant tenant, ApplicationFormV2 applicationForm) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        List<Tenant> otherCoTenants = apartmentSharing.getTenants().stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .toList();

        for (CoTenantForm coTenantForm : applicationForm.getCoTenants()) {
            if (StringUtils.isBlank(coTenantForm.getEmail())) {
                continue;
            }
            String email = coTenantForm.getEmail().toLowerCase();

            Optional<Tenant> matchingCoTenant = otherCoTenants.stream()
                    .filter(t -> Objects.equals(t.getFirstName(), coTenantForm.getFirstName())
                            && Objects.equals(t.getLastName(), coTenantForm.getLastName()))
                    .findFirst();

            if (matchingCoTenant.isPresent()
                    && StringUtils.equalsIgnoreCase(matchingCoTenant.get().getEmail(), email)) {
                continue;
            }

            if (!tenantRepository.findByEmailInAndApartmentSharingNot(List.of(email), apartmentSharing).isEmpty()) {
                return true;
            }
            if (tenantRepository.existsByEmail(email)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDistinctEmails(List<CoTenantForm> coTenantForms) {
        var emails = coTenantForms.stream()
                .filter(t -> StringUtils.isNotBlank(t.getEmail()))
                .collect(Collectors.toList());
        return emails.stream().map(CoTenantForm::getEmail).distinct().count() == emails.size();
    }
}
