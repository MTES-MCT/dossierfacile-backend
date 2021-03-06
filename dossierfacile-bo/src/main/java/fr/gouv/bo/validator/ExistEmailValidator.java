package fr.gouv.bo.validator;


import fr.gouv.bo.repository.UserRepository;
import fr.gouv.bo.validator.annotation.ExistEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class ExistEmailValidator implements ConstraintValidator<ExistEmail, String> {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void initialize(ExistEmail uniqueEmail) {
        //this method is empty
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return userRepository.findOneByEmail(email.toLowerCase()) != null;
    }
}
