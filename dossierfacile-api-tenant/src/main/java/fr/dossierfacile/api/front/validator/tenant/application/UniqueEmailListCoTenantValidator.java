package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.UniqueEmailListCoTenant;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UniqueEmailListCoTenantValidator implements ConstraintValidator<UniqueEmailListCoTenant, List<String>> {

    private final TenantRepository tenantRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(UniqueEmailListCoTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(List<String> emails, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        emails = emails.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<Tenant> tenants = tenantRepository.findByListEmail(emails)
                .stream()
                .filter(t -> t.getApartmentSharing() != null && !t.getApartmentSharing().equals(apartmentSharing)).collect(Collectors.toList());
        return tenants.isEmpty();
    }
}
