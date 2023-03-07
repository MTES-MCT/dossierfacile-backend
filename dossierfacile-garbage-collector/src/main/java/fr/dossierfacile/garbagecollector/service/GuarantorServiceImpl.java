package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.garbagecollector.repo.guarantor.GuarantorRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.GuarantorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GuarantorServiceImpl implements GuarantorService {
    private final GuarantorRepository guarantorRepository;

    @Override
    public void deleteAllGuaratorsAssociatedToTenant(Tenant tenant) {
        guarantorRepository.deleteAll(guarantorRepository.findGuarantorsByTenant(tenant));
    }
}
