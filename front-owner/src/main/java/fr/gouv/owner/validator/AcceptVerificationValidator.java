package fr.gouv.owner.validator;


import fr.gouv.owner.annotation.AcceptVerification;
import fr.gouv.owner.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class AcceptVerificationValidator implements ConstraintValidator<AcceptVerification, Boolean> {

    @Autowired
    TenantRepository tenantRepository;

    @Override
    public void initialize(AcceptVerification acceptVerification) {
        //this method is empty
    }

    @Override
    public boolean isValid(Boolean acceptVerification, ConstraintValidatorContext constraintValidatorContext) {
        return acceptVerification != null ? acceptVerification : Boolean.TRUE;
    }
}
