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

@Deprecated
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
        var existingEmails = tenantRepository.findByEmailInAndApartmentSharingNot(applicationForm.getCoTenantEmail(), tenant.getApartmentSharing())
                .stream()
                .map( t -> t.getEmail())
                .collect(Collectors.toList());

        if (!existingEmails.isEmpty()) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            String msgTemplate = String.format(constraintValidatorContext.getDefaultConstraintMessageTemplate(),
                    String.join(",", existingEmails));
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(msgTemplate)
                    .addPropertyNode("coTenantEmail").addConstraintViolation();
            return false;
        }
        return true;
    }
}
