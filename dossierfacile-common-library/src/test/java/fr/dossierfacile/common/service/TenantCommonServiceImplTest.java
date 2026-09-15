package fr.dossierfacile.common.service;

import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.ApartmentSharingMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantCommonServiceImplTest {

    private TenantCommonRepository tenantCommonRepository;
    private PartnerCallBackService partnerCallBackService;
    private ApartmentSharingCommonService apartmentSharingCommonService;
    private LotteryTicketService lotteryTicketService;

    private TenantCommonServiceImpl service;

    @BeforeEach
    void setUp() {
        tenantCommonRepository = mock(TenantCommonRepository.class);
        partnerCallBackService = mock(PartnerCallBackService.class);
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);
        lotteryTicketService = mock(LotteryTicketService.class);
        TenantMapperForMail tenantMapperForMail = mock(TenantMapperForMail.class);
        ApartmentSharingMapperForMail apartmentSharingMapperForMail = mock(ApartmentSharingMapperForMail.class);
        when(tenantMapperForMail.toDto(any(Tenant.class))).thenReturn(new TenantDto());
        when(apartmentSharingMapperForMail.toDto(any(ApartmentSharing.class))).thenReturn(new ApartmentSharingDto());

        service = new TenantCommonServiceImpl(
                mock(ApartmentSharingRepository.class),
                mock(DocumentCommonRepository.class),
                tenantCommonRepository,
                mock(ApartmentSharingLinkRepository.class),
                partnerCallBackService,
                Optional.empty(),
                tenantMapperForMail,
                apartmentSharingMapperForMail,
                apartmentSharingCommonService,
                lotteryTicketService
        );
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void changeTenantStatusToValidated_resetsTheFullPdfAndNotifiesPartners() {
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).build();
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setApplicationType(ApplicationType.ALONE);
        apartmentSharing.setTenants(new ArrayList<>(List.of(tenant)));
        tenant.setApartmentSharing(apartmentSharing);

        service.changeTenantStatusToValidated(tenant);

        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.VALIDATED);
        verify(tenantCommonRepository).save(tenant);
        // A full PDF rendered with the "not verified" design must not survive the validation
        verify(apartmentSharingCommonService).resetDossierPdfGenerated(apartmentSharing);
        verify(lotteryTicketService).consumeDrawnTicket(1L);
        verify(partnerCallBackService).sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);
    }
}
