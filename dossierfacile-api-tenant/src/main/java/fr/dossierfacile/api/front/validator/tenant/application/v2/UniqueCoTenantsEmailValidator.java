package fr.dossierfacile.api.front.validator.tenant.application.v2;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.v2.UniqueCoTenantsEmail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UniqueCoTenantsEmailValidator implements ConstraintValidator<UniqueCoTenantsEmail, ApplicationFormV2> {

    private final TenantCommonRepository tenantRepository;
    private final TenantService tenantService;

    @Override
    public void initialize(UniqueCoTenantsEmail constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationFormV2 applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        var emails = applicationForm.getCoTenants().stream()
                .map(t -> t.getEmail())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (emails.isEmpty())
            return true;

        var tenant = tenantService.findById(applicationForm.getTenantId());
        if (tenant == null) {
            return true;
        }

        var existingEmails = tenantRepository.findByEmailInAndApartmentSharingNot(emails, tenant.getApartmentSharing())
                .stream()
                .map(t -> t.getEmail())
                .collect(Collectors.toList());

        if (!existingEmails.isEmpty()) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            String msgTemplate = String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(),
                    String.join(",", existingEmails));
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(msgTemplate)
                    .addPropertyNode("coTenants").addConstraintViolation();
            return false;
        }
        return true;
    }
}
