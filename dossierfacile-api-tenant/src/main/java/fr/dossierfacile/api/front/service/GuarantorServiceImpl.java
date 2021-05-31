package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GuarantorServiceImpl implements GuarantorService {
    private final GuarantorRepository guarantorRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public void delete(Long id) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        Guarantor guarantor = guarantorRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new GuarantorNotFoundException(id));
        guarantorRepository.delete(guarantor);
    }
}
