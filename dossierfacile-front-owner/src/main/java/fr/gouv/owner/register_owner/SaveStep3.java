package fr.gouv.owner.register_owner;

import fr.gouv.owner.dto.OwnerDTO;
import fr.dossierfacile.common.entity.Owner;

import fr.dossierfacile.common.entity.Property;

import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.repository.OwnerRepository;
import fr.gouv.owner.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaveStep3 implements SaveStep {
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public Owner saveStep(OwnerDTO ownerDTO) {
        Owner owner = ownerRepository.findBySlug(ownerDTO.getSlug());
        Property property = new Property();
        property.setRentCost(ownerDTO.getCost());
        property.setName(ownerDTO.getAddress());
        property.setOwner(owner);
        owner.setStepRegisterOwner(StepRegisterOwner.STEP3);
        propertyRepository.save(property);
        return ownerRepository.save(owner);
    }
}
