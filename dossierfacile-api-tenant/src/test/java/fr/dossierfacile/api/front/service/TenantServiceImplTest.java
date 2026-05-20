package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private ApartmentSharingRepository apartmentSharingRepository;
    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    @Mock
    private ConfirmationTokenService confirmationTokenService;
    @Mock
    private LogService logService;
    @Mock
    private MailService mailService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private RegisterFactory registerFactory;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private UserApiService userApiService;
    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;
    @Mock
    private TenantMapperForMail tenantMapperForMail;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private ApartmentSharing apartmentSharing;
    private Tenant tenantCreate;
    private Tenant tenantJoin;
    private Document documentOnCreate;
    private Document documentOnJoin;
    private DocumentAnalysisReport reportOnCreate;
    private DocumentAnalysisReport reportOnJoin;

    @BeforeEach
    void setUp() {
        apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(1L);
        apartmentSharing.setApplicationType(ApplicationType.COUPLE);
        apartmentSharing.setTenants(new ArrayList<>());

        tenantCreate = new Tenant();
        tenantCreate.setId(10L);
        tenantCreate.setTenantType(TenantType.CREATE);
        tenantCreate.setApartmentSharing(apartmentSharing);
        tenantCreate.setDocuments(new ArrayList<>());
        tenantCreate.setGuarantors(new ArrayList<>());

        tenantJoin = new Tenant();
        tenantJoin.setId(11L);
        tenantJoin.setTenantType(TenantType.JOIN);
        tenantJoin.setApartmentSharing(apartmentSharing);
        tenantJoin.setDocuments(new ArrayList<>());
        tenantJoin.setGuarantors(new ArrayList<>());

        apartmentSharing.getTenants().add(tenantCreate);
        apartmentSharing.getTenants().add(tenantJoin);

        documentOnCreate = new Document();
        documentOnCreate.setId(100L);
        reportOnCreate = new DocumentAnalysisReport();
        documentOnCreate.setDocumentAnalysisReport(reportOnCreate);
        tenantCreate.getDocuments().add(documentOnCreate);

        documentOnJoin = new Document();
        documentOnJoin.setId(101L);
        reportOnJoin = new DocumentAnalysisReport();
        documentOnJoin.setDocumentAnalysisReport(reportOnJoin);
        tenantJoin.getDocuments().add(documentOnJoin);
    }

    @Test
    void addCommentAnalysis_successForOwnDocument() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(documentOnCreate));
        when(documentService.hasPermissionOnDocument(documentOnCreate, tenantCreate)).thenReturn(true);

        tenantService.addCommentAnalysis(tenantCreate, 100L, "Own comment");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnCreate);
        assertEquals("Own comment", reportOnCreate.getComment());
    }

    @Test
    void addCommentAnalysis_successForCoupleSecondaryDocumentWhenPrimary() {
        when(documentRepository.findById(101L)).thenReturn(Optional.of(documentOnJoin));
        when(documentService.hasPermissionOnDocument(documentOnJoin, tenantCreate)).thenReturn(true);

        // Tenant CREATE can access Tenant JOIN's documents
        tenantService.addCommentAnalysis(tenantCreate, 101L, "Primary commenting secondary");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnJoin);
        assertEquals("Primary commenting secondary", reportOnJoin.getComment());
    }

    @Test
    void addCommentAnalysis_successForCouplePrimaryDocumentWhenSecondary() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(documentOnCreate));
        when(documentService.hasPermissionOnDocument(documentOnCreate, tenantJoin)).thenReturn(true);

        // Tenant JOIN should be able to access Tenant CREATE's documents.
        tenantService.addCommentAnalysis(tenantJoin, 100L, "Secondary commenting primary");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnCreate);
        assertEquals("Secondary commenting primary", reportOnCreate.getComment());
    }

}
