package fr.gouv.owner.validator;

import fr.gouv.owner.annotation.JoinApartmentSharingCapacity;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.owner.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class JoinApartmentSharingCapacityValidator implements ConstraintValidator<JoinApartmentSharingCapacity, String> {


    @Autowired
    TenantRepository tenantRepository;

    @Override
    public void initialize(JoinApartmentSharingCapacity joinApartmentSharingCapacity) {
        //this method is empty
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = tenantRepository.findOneByEmail(s);
        if (tenant == null) {
            return true;
        }
        return tenant.getApartmentSharing().getTenants().size() < tenant.getApartmentSharing().getNumberOfTenants();
    }
}
