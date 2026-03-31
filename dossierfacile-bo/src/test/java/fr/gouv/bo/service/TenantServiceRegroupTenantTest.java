package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TenantServiceRegroupTenantTest {

    private BOApartmentSharingRepository apartmentSharingRepository;
    private ApartmentSharingService apartmentSharingService;
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        apartmentSharingRepository = mock(BOApartmentSharingRepository.class);
        apartmentSharingService = mock(ApartmentSharingService.class);
        tenantService = new TenantService(
                mock(), // tenantRepository
                null,   // mailService
                null,   // partnerCallBackService
                null,   // userService
                null,   // messageSource
                null,   // documentRepository
                null,   // documentDeniedReasonsRepository
                null,   // messageService
                apartmentSharingRepository,
                null,   // operatorLogRepository
                null,   // documentDeniedReasonsService
                null,   // documentService
                null,   // tenantLogService
                null,   // keycloakService
                apartmentSharingService,
                null,   // guarantorRepository
                null,   // tenantMapperForMail
                null,   // apartmentSharingMapperForMail
                null,   // tenantCommonService
                null    // tenantLogCommonService
        );
    }

    @Test
    void regroupTenant_shouldResetDossierPdf() {
        // Target apartment with a completed PDF
        StorageFile existingPdf = StorageFile.builder().id(1L).build();
        Tenant tenantCreate = Tenant.builder().id(1L).build();
        ApartmentSharing targetApartment = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .dossierPdfDocumentStatus(FileStatus.COMPLETED)
                .pdfDossierFile(existingPdf)
                .tenants(new ArrayList<>(List.of(tenantCreate)))
                .build();
        tenantCreate.setApartmentSharing(targetApartment);

        // Source apartment (tenant alone who will be moved)
        Tenant tenantJoin = Tenant.builder().id(2L).build();
        ApartmentSharing sourceApartment = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .tenants(new ArrayList<>(List.of(tenantJoin)))
                .build();
        tenantJoin.setApartmentSharing(sourceApartment);

        // Action
        tenantService.regroupTenant(tenantJoin, targetApartment, ApplicationType.GROUP);

        // PDF must have been invalidated on the target apartment
        verify(apartmentSharingService).resetDossierPdfGenerated(targetApartment);

        // Tenant should have been moved
        assertThat(tenantJoin.getApartmentSharing()).isEqualTo(targetApartment);
        assertThat(tenantJoin.getTenantType()).isEqualTo(TenantType.JOIN);
        assertThat(targetApartment.getTenants()).contains(tenantJoin);
    }
}
