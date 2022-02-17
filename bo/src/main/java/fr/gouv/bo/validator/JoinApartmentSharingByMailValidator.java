package fr.gouv.bo.validator;


import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.gouv.bo.validator.annotation.JoinApartmentSharingByMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class JoinApartmentSharingByMailValidator implements ConstraintValidator<JoinApartmentSharingByMail, String> {

    @Autowired
    TenantCommonRepository tenantRepository;

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
