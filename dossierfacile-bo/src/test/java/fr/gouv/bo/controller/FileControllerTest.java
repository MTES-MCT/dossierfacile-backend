package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.SharedFileService;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.security.BOAccessDenied;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private static final Long FILE_ID = 12L;
    private static final Long TENANT_ID = 42L;

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SharedFileService fileService;
    @Mock
    private BOApplicationAccessService applicationAccessService;

    private FileController controller;

    @BeforeEach
    void setUp() {
        controller = new FileController(
                documentRepository,
                fileStorageService,
                fileService,
                applicationAccessService
        );
    }

    @Nested
    class GetDocumentAsByteArray {

        private static final String DOCUMENT_NAME = "watermarked-doc.pdf";

        @Test
        void whenDocumentDoesNotExist_returns404() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(documentRepository.findByName(DOCUMENT_NAME)).thenReturn(Optional.empty());

            controller.getDocumentAsByteArray(response, DOCUMENT_NAME, operatorPrincipal());

            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void whenOperatorNotAssigned_throwsAccessDenied() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            UserPrincipal principal = operatorPrincipal();
            Document document = documentWithWatermark();
            when(documentRepository.findByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkDocumentAccess(principal, document);

            assertThatThrownBy(() -> controller.getDocumentAsByteArray(response, DOCUMENT_NAME, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }

        @Test
        void whenOperatorAssigned_streamsWatermark() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            UserPrincipal principal = operatorPrincipal();
            Document document = documentWithWatermark();
            when(documentRepository.findByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));
            when(fileStorageService.download(document.getWatermarkFile()))
                    .thenReturn(new ByteArrayInputStream("watermark".getBytes()));

            controller.getDocumentAsByteArray(response, DOCUMENT_NAME, principal);

            verify(applicationAccessService).checkDocumentAccess(principal, document);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentType()).isEqualTo("application/pdf");
            assertThat(response.getContentAsByteArray()).isEqualTo("watermark".getBytes());
        }
    }

    @Nested
    class GetPreviewFileAsByteArray {

        @Test
        void whenFileDoesNotExist_returns404() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(fileService.findById(FILE_ID)).thenReturn(Optional.empty());

            controller.getPreviewFileAsByteArray(response, FILE_ID, operatorPrincipal());

            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void whenOperatorNotAssigned_throwsAccessDenied() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            UserPrincipal principal = operatorPrincipal();
            File file = fileWithPreview();
            when(fileService.findById(FILE_ID)).thenReturn(Optional.of(file));
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkFileAccess(principal, file);

            assertThatThrownBy(() -> controller.getPreviewFileAsByteArray(response, FILE_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }

        @Test
        void whenOperatorAssignedAndPreviewExists_streamsPreview() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            UserPrincipal principal = operatorPrincipal();
            File file = fileWithPreview();
            when(fileService.findById(FILE_ID)).thenReturn(Optional.of(file));
            when(fileStorageService.download(file.getPreview()))
                    .thenReturn(new ByteArrayInputStream("preview".getBytes()));

            controller.getPreviewFileAsByteArray(response, FILE_ID, principal);

            verify(applicationAccessService).checkFileAccess(principal, file);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentType()).isEqualTo("image/jpeg");
            assertThat(response.getContentAsByteArray()).isEqualTo("preview".getBytes());
        }

        @Test
        void whenOperatorAssignedButPreviewMissing_returns404() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            UserPrincipal principal = operatorPrincipal();
            File file = fileWithoutPreview();
            when(fileService.findById(FILE_ID)).thenReturn(Optional.of(file));

            controller.getPreviewFileAsByteArray(response, FILE_ID, principal);

            verify(applicationAccessService).checkFileAccess(principal, file);
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    private File fileWithPreview() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        Document document = Document.builder().id(1L).tenant(tenant).build();
        StorageFile preview = new StorageFile();
        preview.setContentType("image/jpeg");
        return File.builder().id(FILE_ID).document(document).preview(preview).build();
    }

    private File fileWithoutPreview() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        Document document = Document.builder().id(1L).tenant(tenant).build();
        return File.builder().id(FILE_ID).document(document).build();
    }

    private Document documentWithWatermark() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        StorageFile watermark = new StorageFile();
        watermark.setContentType("application/pdf");
        return Document.builder().id(1L).tenant(tenant).name("watermarked-doc.pdf").watermarkFile(watermark).build();
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(10L, "operator", "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }
}
