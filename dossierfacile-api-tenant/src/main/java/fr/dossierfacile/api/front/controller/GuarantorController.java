package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.common.entity.Guarantor;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/guarantor")
public class GuarantorController {
    private final GuarantorService guarantorService;
    private final AuthenticationFacade authenticationFacade;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var tenant = authenticationFacade.getTenant(null);
        try {
            guarantorService.delete(id, tenant);
        } catch (GuarantorNotFoundException e) {
            // try to find it on conTenant
            Guarantor g = guarantorService.findById(id);
            tenant = tenant.getApartmentSharing().getTenants().stream().filter(
                    (t) -> t.getId().equals(g.getTenant().getId())
            ).findFirst().orElseThrow(GuarantorNotFoundException::new);
            guarantorService.delete(id, tenant);
        }
        return ResponseEntity.ok().build();
    }

}
