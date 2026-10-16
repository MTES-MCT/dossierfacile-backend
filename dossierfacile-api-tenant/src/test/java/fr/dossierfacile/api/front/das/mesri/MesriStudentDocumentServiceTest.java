package fr.dossierfacile.api.front.das.mesri;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesriStudentDocumentServiceTest {

    @Mock
    private MesriStudentEligibility eligibility;
    @Mock
    private FranceConnectTokenService franceConnectTokenService;
    @Mock
    private ApiParticulierClient apiParticulierClient;
    @Mock
    private MesriStudentStatusPdfRenderer pdfRenderer;
    @Mock
    private TenantService tenantService;
    @Mock
    private MesriStudentLogService mesriStudentLogService;

    @InjectMocks
    private MesriStudentDocumentService service;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(42L);
        tenant.setFirstName("Angela");
        tenant.setLastName("DUBOIS");
    }

    @Test
    void savesStudentDocumentThroughRegularUploadPath() throws Exception {
        FranceConnectAccessToken token = new FranceConnectAccessToken("fc", "openid mesri_admissions", null);
        MesriStudentStatus status = new MesriStudentStatus("{}", "url", "siret",
                List.of(new MesriStudentStatus.Admission("2020-07-01", "2021-08-31", true, "RF1", "formation initiale", "33063", "0330023W", "LYCEE")));
        TenantModel expected = TenantModel.builder().id(42L).build();
        when(franceConnectTokenService.getFranceConnectAccessToken("jwt")).thenReturn(token);
        when(apiParticulierClient.fetchStudentStatus("fc")).thenReturn(status);
        when(pdfRenderer.render(eq(status), eq("Angela DUBOIS"), any())).thenReturn("%PDF".getBytes());
        when(tenantService.saveStepRegister(eq(tenant), any(DocumentProfessionalForm.class), eq(StepRegister.DOCUMENT_PROFESSIONAL))).thenReturn(expected);

        TenantModel result = service.fetchAndSaveStudentDocument(tenant, "jwt");

        assertThat(result).isSameAs(expected);
        verify(eligibility).check(tenant);
        ArgumentCaptor<DocumentProfessionalForm> captor = ArgumentCaptor.forClass(DocumentProfessionalForm.class);
        verify(tenantService).saveStepRegister(eq(tenant), captor.capture(), eq(StepRegister.DOCUMENT_PROFESSIONAL));
        DocumentProfessionalForm form = captor.getValue();
        assertThat(form.getTypeDocumentProfessional()).isEqualTo(DocumentSubCategory.STUDENT);
        assertThat(form.getDocuments()).hasSize(1);
        assertThat(form.getDocuments().get(0).getOriginalFilename()).isEqualTo("statut-etudiant-franceconnect.pdf");
        assertThat(form.getDocuments().get(0).getContentType()).isEqualTo("application/pdf");
        assertThat(form.getDocuments().get(0).getBytes()).isEqualTo("%PDF".getBytes());
        verify(mesriStudentLogService).logSuccess(42L, status, token);
        verify(mesriStudentLogService, never()).logFailure(any(), any());
    }

    @Test
    void notEligibleIsForbiddenAndCallsNothing() {
        doThrow(new MesriNotEligibleException("flag off")).when(eligibility).check(tenant);

        assertThatThrownBy(() -> service.fetchAndSaveStudentDocument(tenant, "jwt")).isInstanceOf(MesriNotEligibleException.class);
        verifyNoInteractions(franceConnectTokenService, apiParticulierClient, tenantService, mesriStudentLogService);
    }

    @Test
    void apiFailureIsLoggedAndRethrownWithoutSavingAnything() {
        when(franceConnectTokenService.getFranceConnectAccessToken("jwt")).thenReturn(new FranceConnectAccessToken("fc", null, null));
        when(apiParticulierClient.fetchStudentStatus("fc")).thenThrow(new MesriStudentNotFoundException("25003"));

        assertThatThrownBy(() -> service.fetchAndSaveStudentDocument(tenant, "jwt")).isInstanceOf(MesriStudentNotFoundException.class);

        verify(mesriStudentLogService).logFailure(eq(42L), any(MesriStudentNotFoundException.class));
        verify(tenantService, never()).saveStepRegister(any(), any(), any());
        verify(mesriStudentLogService, never()).logSuccess(any(), any(), any());
    }

    @Test
    void expiredFranceConnectTokenIsLoggedAndRethrown() {
        when(franceConnectTokenService.getFranceConnectAccessToken(anyString()))
                .thenThrow(new FranceConnectTokenUnavailableException("expired"));

        assertThatThrownBy(() -> service.fetchAndSaveStudentDocument(tenant, "jwt")).isInstanceOf(FranceConnectTokenUnavailableException.class);
        verify(mesriStudentLogService).logFailure(eq(42L), any(FranceConnectTokenUnavailableException.class));
        verifyNoInteractions(apiParticulierClient);
    }
}
