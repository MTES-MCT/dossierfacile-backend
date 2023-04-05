package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.api.dossierfacileapiowner.register.KeycloakService;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class OwnerServiceImpl implements OwnerService {
    private final OwnerRepository ownerRepository;
    private final AuthenticationFacade authenticationFacade;
    private final OwnerMapper ownerMapper;
    private final LogService logService;
    private final KeycloakService keycloakService;

    @Override
    public OwnerModel setNames(NamesForm namesForm) {
        Owner owner = authenticationFacade.getOwner();
        owner.setFirstName(namesForm.getFirstName());
        owner.setLastName(namesForm.getLastName());
        owner.setEmail(namesForm.getEmail());
        return ownerMapper.toOwnerModel(ownerRepository.save(owner));
    }

    @Override
    public void deleteAccount(Owner owner) {
        log.info("Removing owner with id [" + owner.getId() + "]");
        logService.saveLog(LogType.ACCOUNT_DELETE, owner.getId());
        keycloakService.deleteKeycloakUser(owner);
        ownerRepository.delete(owner);
    }

    @Override
    public void updateLastLoginDate(Owner owner) {
        owner.setLastLoginDate(LocalDateTime.now());
        ownerRepository.save(owner);
    }

}
