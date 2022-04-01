package fr.gouv.owner.register_owner;

import fr.gouv.owner.dto.OwnerDTO;
import fr.dossierfacile.common.entity.Owner;

import fr.dossierfacile.common.entity.UserRole;

import fr.dossierfacile.common.enums.StepRegisterOwner;

import fr.gouv.owner.repository.OwnerRepository;

import fr.gouv.owner.repository.UserRoleRepository;

import fr.gouv.owner.service.MailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SaveStep2 implements SaveStep {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private MailService mailService;

    @Override
    public Owner saveStep(OwnerDTO ownerDTO) {
        Owner owner = ownerRepository.findBySlug(ownerDTO.getSlug());
        owner.setEmail(ownerDTO.getEmail());
        owner.setPassword(bCryptPasswordEncoder.encode(ownerDTO.getPassword()));
        UserRole userRole = new UserRole(owner);
        owner.setStepRegisterOwner(StepRegisterOwner.STEP2);
        mailService.sendEmailOwnerWelcome(owner);
        userRoleRepository.save(userRole);
        return ownerRepository.save(owner);
    }
}
