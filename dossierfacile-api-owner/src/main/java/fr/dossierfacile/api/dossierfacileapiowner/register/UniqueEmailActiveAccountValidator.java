package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UniqueEmailActiveAccountValidator implements ConstraintValidator<UniqueEmailActiveAccount, String> {

    private final OwnerRepository ownerRepository;

    @Override
    public void initialize(UniqueEmailActiveAccount uniqueEmailActiveAccount) {
        //this method is empty
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return ownerRepository.findOneByEmailAndEnabledTrue(email) == null && !email.isEmpty();
    }
}
