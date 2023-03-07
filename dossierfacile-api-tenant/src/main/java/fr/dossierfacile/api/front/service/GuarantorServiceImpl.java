package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GuarantorServiceImpl implements GuarantorService {
    private final GuarantorRepository guarantorRepository;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    public void delete(Long id, Tenant tenant) {
        Guarantor guarantor = guarantorRepository.findByIdForApartmentSharing(id, tenant.getApartmentSharing().getId())
                .orElseThrow(() -> new GuarantorNotFoundException(id));
        guarantorRepository.delete(guarantor);
        guarantorRepository.flush();
        tenantStatusService.updateTenantStatus(guarantor.getTenant());
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Override
    public Guarantor findById(Long id) {
        return guarantorRepository.findById(id).orElseThrow(GuarantorNotFoundException::new);
    }
}
