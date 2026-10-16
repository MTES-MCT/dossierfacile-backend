package fr.dossierfacile.api.front.das.mesri;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * POC "statut étudiant à la source": reads the FranceConnect token stored by Keycloak, asks API Particulier
 * for the student status, renders a PDF and saves it as a PROFESSIONAL / STUDENT document through the
 * regular upload path (process-file, watermark, status reset, dossier PDF).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MesriStudentDocumentService {

    static final String FILE_NAME = "statut-etudiant-franceconnect.pdf";

    private final MesriStudentEligibility eligibility;
    private final FranceConnectTokenService franceConnectTokenService;
    private final ApiParticulierClient apiParticulierClient;
    private final MesriStudentStatusPdfRenderer pdfRenderer;
    private final TenantService tenantService;
    private final MesriStudentLogService mesriStudentLogService;

    public TenantModel fetchAndSaveStudentDocument(Tenant tenant, String userJwt) {
        eligibility.check(tenant);
        try {
            FranceConnectAccessToken token = franceConnectTokenService.getFranceConnectAccessToken(userJwt);
            MesriStudentStatus status = apiParticulierClient.fetchStudentStatus(token.accessToken());
            byte[] pdf = pdfRenderer.render(status, fullName(tenant), LocalDateTime.now());

            DocumentProfessionalForm form = new DocumentProfessionalForm();
            form.setTypeDocumentProfessional(DocumentSubCategory.STUDENT);
            form.setDocuments(List.of(new ByteArrayMultipartFile("documents", FILE_NAME, MediaType.APPLICATION_PDF_VALUE, pdf)));
            TenantModel model = tenantService.saveStepRegister(tenant, form, StepRegister.DOCUMENT_PROFESSIONAL);

            mesriStudentLogService.logSuccess(tenant.getId(), status, token);
            log.info("MESR student status document saved for tenant {} ({} admissions)", tenant.getId(), status.admissions().size());
            return model;
        } catch (IOException e) {
            ApiParticulierException wrapped = new ApiParticulierException("Could not render the student status PDF: " + e.getMessage());
            mesriStudentLogService.logFailure(tenant.getId(), wrapped);
            throw wrapped;
        } catch (RuntimeException e) {
            log.warn("MESR student status retrieval failed for tenant {}: {}", tenant.getId(), e.getMessage());
            mesriStudentLogService.logFailure(tenant.getId(), e);
            throw e;
        }
    }

    private static String fullName(Tenant tenant) {
        return ((tenant.getFirstName() == null ? "" : tenant.getFirstName()) + " "
                + (tenant.getLastName() == null ? "" : tenant.getLastName())).trim();
    }
}
