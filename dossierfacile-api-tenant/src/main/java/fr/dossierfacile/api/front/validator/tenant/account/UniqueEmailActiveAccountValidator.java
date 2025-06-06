package fr.dossierfacile.api.front.validator.tenant.account;

import fr.dossierfacile.api.front.validator.annotation.tenant.account.UniqueEmailActiveAccount;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UniqueEmailActiveAccountValidator implements ConstraintValidator<UniqueEmailActiveAccount, String> {

    private final TenantCommonRepository tenantRepository;

    @Override
    public void initialize(UniqueEmailActiveAccount uniqueEmailActiveAccount) {
        //this method is empty
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return !tenantRepository.existsByEmail(email.toLowerCase()) && !email.isEmpty();
    }
}
