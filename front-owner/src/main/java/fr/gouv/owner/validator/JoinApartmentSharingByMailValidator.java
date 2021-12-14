package fr.gouv.owner.validator;


import fr.gouv.owner.annotation.JoinApartmentSharingByMail;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.owner.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class JoinApartmentSharingByMailValidator implements ConstraintValidator<JoinApartmentSharingByMail, String> {

    @Autowired
    TenantRepository tenantRepository;

    @Override
    public void initialize(JoinApartmentSharingByMail joinApartmentSharingByMail) {
        //this method is empty
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = tenantRepository.findOneByEmail(email.toLowerCase());
        return tenant != null && tenant.getApartmentSharing() != null;
    }
}
