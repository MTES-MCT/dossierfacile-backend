package fr.dossierfacile.common.application.service;

import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDeletionProcessServiceTest {

    private FileDeletionProcessService service;

    @Mock
    private FileDeletionDomainService fileDeletionDomainService;
    @Mock
    private JpaApartmentSharingRepository jpaApartmentSharingRepository;
    @Mock
    private JpaTenantRepository jpaTenantRepository;

    @BeforeEach
    void setUp() {
        service = new FileDeletionProcessService(
                fileDeletionDomainService,
                jpaApartmentSharingRepository,
                jpaTenantRepository
        );
    }

    @Test
    void should_process_file_deletion_successfully() {
        // Given
        Long fileId = 123L;
        Document document = mock(Document.class);
        Tenant tenant = mock(Tenant.class);
        ApartmentSharing apartmentSharing = mock(ApartmentSharing.class);
        Optional<Operator> operator = Optional.empty();

        when(fileDeletionDomainService.deleteFile(fileId, document, tenant, operator))
                .thenReturn(Optional.of(document));

        // When
        Optional<Document> result = service.processFileDeletion(fileId, document, tenant, apartmentSharing, operator);

        // Then
        assertThat(result).isPresent().contains(document);

        verify(fileDeletionDomainService).deleteFile(fileId, document, tenant, operator);
        verify(apartmentSharing).resetDossierPdfGenerated();
        verify(jpaApartmentSharingRepository).save(apartmentSharing);
        verify(tenant).updateLastUpdateDate();
        verify(jpaTenantRepository).save(tenant);
    }
}
