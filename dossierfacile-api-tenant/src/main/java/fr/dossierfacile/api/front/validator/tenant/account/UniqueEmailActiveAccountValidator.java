package fr.dossierfacile.api.front.validator.tenant.account;

import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.validator.anotation.tenant.account.UniqueEmailActiveAccount;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@AllArgsConstructor
public class UniqueEmailActiveAccountValidator implements ConstraintValidator<UniqueEmailActiveAccount, String> {

    private final UserRepository userRepository;

    @Override
    public void initialize(UniqueEmailActiveAccount uniqueEmailActiveAccount) {
        //this method is empty
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return userRepository.findOneByEmailAndEnabledTrue(email.toLowerCase()) == null && !email.isEmpty();
    }
}
