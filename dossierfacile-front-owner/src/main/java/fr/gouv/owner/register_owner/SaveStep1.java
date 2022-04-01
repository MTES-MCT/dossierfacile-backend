package fr.gouv.owner.register_owner;

import fr.gouv.owner.dto.OwnerDTO;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.repository.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaveStep1 implements SaveStep {
    @Autowired
    private OwnerRepository ownerRepository;

    @Override
    public Owner saveStep(OwnerDTO ownerDTO) {
        Owner owner = new Owner();
        owner.setFirstName(ownerDTO.getFirstName());
        owner.setLastName(ownerDTO.getLastName());
        owner.setStepRegisterOwner(StepRegisterOwner.STEP1);
        return ownerRepository.save(owner);
    }
}
