package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.UniqueEmailListCoTenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UniqueEmailListCoTenantValidator implements ConstraintValidator<UniqueEmailListCoTenant, ApplicationForm> {

    private final TenantCommonRepository tenantRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(UniqueEmailListCoTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationForm applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = authenticationFacade.getTenant(applicationForm.getTenantId());
        if (tenant == null) {
            return true;
        }
        var apartmentSharing = tenant.getApartmentSharing();
        var emails = applicationForm.getCoTenantEmail().stream().map(String::toLowerCase).collect(Collectors.toList());
        var tenants = tenantRepository.findByListEmail(emails)
                .stream()
                .filter(t -> t.getApartmentSharing() != null && !t.getApartmentSharing().equals(apartmentSharing)).collect(Collectors.toList());
        boolean isValid = tenants.isEmpty();
        if (!isValid) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("coTenantEmail").addConstraintViolation();
        }
        return isValid;
    }
}
