package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.api.dossierfacileapiowner.log.LogService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.api.dossierfacileapiowner.register.KeycloakService;
import fr.dossierfacile.common.entity.Owner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/owner")
@Slf4j
public class OwnerController {

    private final LogService logService;
    private final OwnerService ownerService;
    private final AuthenticationFacade authenticationFacade;
    private final KeycloakService keycloakService;
    private final OwnerMapper ownerMapper;

    @PostMapping("/names")
    public ResponseEntity<OwnerModel> names(@RequestBody NamesForm namesForm) {
        OwnerModel ownerModel = ownerService.setNames(namesForm);
        return ok(ownerModel);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Owner owner = authenticationFacade.getOwner();
        keycloakService.logout(owner);
        return ok().build();
    }

    @GetMapping(value = "/profile")
    public ResponseEntity<OwnerModel> profile() {
        Owner owner = authenticationFacade.getOwner();
        return ok(ownerMapper.toOwnerModel(owner));
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Void> deleteAccount() {
        Owner owner = authenticationFacade.getOwner();
        ownerService.deleteAccount(owner);
        return ok().build();
    }


}
