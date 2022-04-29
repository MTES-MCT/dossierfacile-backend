package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
        return ownerRepository.findOneByEmailAndEnabledTrue(email.toLowerCase()) == null && !email.isEmpty();
    }
}
