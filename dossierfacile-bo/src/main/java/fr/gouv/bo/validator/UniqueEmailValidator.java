package fr.gouv.bo.validator;


import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.validator.annotation.UniqueEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    @Autowired
    private BOUserRepository userRepository;

    @Override
    public void initialize(UniqueEmail uniqueEmail) {
        //this method is empty
    }


    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return userRepository.findByEmail(email.toLowerCase()).isEmpty();
    }
}
