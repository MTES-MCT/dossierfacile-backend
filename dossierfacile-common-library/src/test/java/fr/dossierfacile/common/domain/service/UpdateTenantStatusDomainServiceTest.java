package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.GuarantorEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateTenantStatusDomainServiceTest {

    private UpdateTenantStatusDomainService service;

    @Mock
    private JpaTenantRepository jpaTenantRepository;

    @Mock
    private JpaDocumentRepository jpaDocumentRepository;

    @Mock
    private JpaGuarantorRepository jpaGuarantorRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private AddLogDomainService addLogDomainService;

    @BeforeEach
    void setUp() {
        service = new UpdateTenantStatusDomainService(jpaTenantRepository, jpaDocumentRepository, jpaGuarantorRepository, eventPublisher, addLogDomainService);
    }

    @Nested
    @DisplayName("TDD Tests for computeTenantStatus()")
    class ComputeTenantStatusTest {

        private Document buildDocument(DocumentCategory category, DocumentStatus status) {
            return new Document(DocumentEntity.builder()
                    .documentCategory(category)
                    .documentStatus(status)
                    .build());
        }

        private Document buildGuarantorDocument(Long guarantorId, DocumentCategory category, DocumentStatus status) {
            return new Document(DocumentEntity.builder()
                    .guarantorId(guarantorId)
                    .documentCategory(category)
                    .documentStatus(status)
                    .build());
        }

        private List<Document> buildTenantMandatoryDocuments(DocumentStatus status) {
            return new ArrayList<>(List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, status),
                    buildDocument(DocumentCategory.RESIDENCY, status),
                    buildDocument(DocumentCategory.PROFESSIONAL, status),
                    buildDocument(DocumentCategory.FINANCIAL, status),
                    buildDocument(DocumentCategory.TAX, status)
            ));
        }

        private List<Document> buildGuarantorMandatoryDocuments(Long guarantorId, DocumentStatus status) {
            return new ArrayList<>(List.of(
                    buildGuarantorDocument(guarantorId, DocumentCategory.IDENTIFICATION, status),
                    buildGuarantorDocument(guarantorId, DocumentCategory.RESIDENCY, status),
                    buildGuarantorDocument(guarantorId, DocumentCategory.PROFESSIONAL, status),
                    buildGuarantorDocument(guarantorId, DocumentCategory.FINANCIAL, status),
                    buildGuarantorDocument(guarantorId, DocumentCategory.TAX, status)
            ));
        }

        @Test
        @DisplayName("Should return ARCHIVED when status is ARCHIVED")
        void should_return_archived_when_status_is_archived() {
            TenantEntity entity = TenantEntity.builder().id(1L).status(TenantFileStatus.ARCHIVED).build();
            Tenant tenant = new Tenant(entity);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should return DECLINED when any document is DECLINED")
        void should_return_declined_when_any_document_is_declined() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> docs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            docs.add(buildDocument(DocumentCategory.FINANCIAL, DocumentStatus.DECLINED));

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(docs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.DECLINED);
        }

        @Test
        @DisplayName("Should return DECLINED when guarantor document is DECLINED")
        void should_return_declined_when_guarantor_document_is_declined() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildGuarantorDocument(100L, DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE, DocumentStatus.DECLINED));

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.ORGANISM).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.DECLINED);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when honorDeclaration is false")
        void should_return_incomplete_when_honor_declaration_is_false() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(false).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> docs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(docs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when mandatory categories are missing on tenant")
        void should_return_incomplete_when_tenant_missing_categories() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> incompleteDocs = List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.RESIDENCY, DocumentStatus.VALIDATED)
            );

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(incompleteDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor ORGANISM does not have exact certificate")
        void should_return_incomplete_when_organism_guarantor_missing_certificate() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildGuarantorDocument(100L, DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.ORGANISM).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor NATURAL_PERSON is missing mandatory categories")
        void should_return_incomplete_when_natural_person_guarantor_missing_categories() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildGuarantorDocument(100L, DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.NATURAL_PERSON).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor LEGAL_PERSON is missing mandatory categories")
        void should_return_incomplete_when_legal_person_guarantor_missing_categories() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildGuarantorDocument(100L, DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.LEGAL_PERSON).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return TO_PROCESS when honorDeclaration is true, categories present, and any document is TO_PROCESS")
        void should_return_to_process_when_document_is_to_process() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = new ArrayList<>(List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.TO_PROCESS),
                    buildDocument(DocumentCategory.RESIDENCY, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.PROFESSIONAL, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.FINANCIAL, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.TAX, DocumentStatus.VALIDATED)
            ));

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.TO_PROCESS);
        }

        @Test
        @DisplayName("Should return VALIDATED when honorDeclaration is true, categories present, and all documents are VALIDATED")
        void should_return_validated_when_all_conditions_met_without_guarantor() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.VALIDATED);
        }

        @Test
        @DisplayName("Should return VALIDATED when tenant and NATURAL_PERSON guarantor are fully complete and VALIDATED")
        void should_return_validated_with_natural_person_guarantor() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = buildGuarantorMandatoryDocuments(100L, DocumentStatus.VALIDATED);

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.NATURAL_PERSON).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.VALIDATED);
        }

        @Test
        @DisplayName("Should return VALIDATED when tenant and LEGAL_PERSON guarantor are fully complete and VALIDATED")
        void should_return_validated_with_legal_person_guarantor() {
            TenantEntity entity = TenantEntity.builder().id(1L).honorDeclaration(true).status(TenantFileStatus.TO_PROCESS).build();
            Tenant tenant = new Tenant(entity);

            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(
                    buildGuarantorDocument(100L, DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED),
                    buildGuarantorDocument(100L, DocumentCategory.IDENTIFICATION_LEGAL_PERSON, DocumentStatus.VALIDATED)
            );

            Guarantor guarantor = new Guarantor(GuarantorEntity.builder().id(100L).typeGuarantor(TypeGuarantor.LEGAL_PERSON).build());

            when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
            when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of(guarantor));
            when(jpaDocumentRepository.getDocumentsByGuarantorsIds(List.of(100L))).thenReturn(guarantorDocs);

            assertThat(service.computeTenantStatus(tenant)).isEqualTo(TenantFileStatus.VALIDATED);
        }
    }

    @Nested
    @DisplayName("Tests for updateTenantStatus()")
    class UpdateTenantStatusTest {

        private void setupToReturnStatus(TenantFileStatus targetStatus) {
            if (targetStatus == TenantFileStatus.VALIDATED) {
                List<Document> tenantDocs = new ArrayList<>(List.of(
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.IDENTIFICATION).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.RESIDENCY).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.PROFESSIONAL).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.FINANCIAL).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.TAX).documentStatus(DocumentStatus.VALIDATED).build())
                ));
                when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
                when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
                when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());
            } else if (targetStatus == TenantFileStatus.DECLINED) {
                List<Document> tenantDocs = new ArrayList<>(List.of(
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.IDENTIFICATION).documentStatus(DocumentStatus.DECLINED).build())
                ));
                when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
                when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
                when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());
            } else if (targetStatus == TenantFileStatus.TO_PROCESS) {
                List<Document> tenantDocs = new ArrayList<>(List.of(
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.IDENTIFICATION).documentStatus(DocumentStatus.TO_PROCESS).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.RESIDENCY).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.PROFESSIONAL).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.FINANCIAL).documentStatus(DocumentStatus.VALIDATED).build()),
                        new Document(DocumentEntity.builder().documentCategory(DocumentCategory.TAX).documentStatus(DocumentStatus.VALIDATED).build())
                ));
                when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(tenantDocs);
                when(jpaGuarantorRepository.findByTenantId(1L)).thenReturn(List.of());
                when(jpaDocumentRepository.getDocumentsByGuarantorsIds(anyList())).thenReturn(List.of());
            }
        }

        @Test
        @DisplayName("Should update status, save, log and publish event when status changes to VALIDATED")
        void should_update_save_log_and_publish_when_status_changes_to_validated() {
            TenantEntity entity = TenantEntity.builder().id(1L).status(TenantFileStatus.TO_PROCESS).honorDeclaration(true).build();
            Tenant tenant = new Tenant(entity);
            fr.dossierfacile.common.entity.User operator = mock(fr.dossierfacile.common.entity.User.class);

            setupToReturnStatus(TenantFileStatus.VALIDATED);

            var result = service.updateTenantStatus(tenant, operator);

            assertThat(result.hasBeenUpdated()).isTrue();
            assertThat(result.newStatus()).isEqualTo(TenantFileStatus.VALIDATED);
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.VALIDATED);

            verify(jpaTenantRepository).save(tenant);
            verify(addLogDomainService).addAccountValidatedLog(tenant, java.util.Optional.of(operator));
            verify(eventPublisher).publishEvent(any(fr.dossierfacile.common.domain.event.TenantStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Should update status, save, log and publish event when status changes to DECLINED")
        void should_update_save_log_and_publish_when_status_changes_to_declined() {
            TenantEntity entity = TenantEntity.builder().id(1L).status(TenantFileStatus.TO_PROCESS).honorDeclaration(true).build();
            Tenant tenant = new Tenant(entity);
            fr.dossierfacile.common.entity.User operator = mock(fr.dossierfacile.common.entity.User.class);

            setupToReturnStatus(TenantFileStatus.DECLINED);

            var result = service.updateTenantStatus(tenant, operator);

            assertThat(result.hasBeenUpdated()).isTrue();
            assertThat(result.newStatus()).isEqualTo(TenantFileStatus.DECLINED);
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.DECLINED);

            verify(jpaTenantRepository).save(tenant);
            verify(addLogDomainService).addAccountDeniedLog(tenant, java.util.Optional.of(operator));
            verify(eventPublisher).publishEvent(any(fr.dossierfacile.common.domain.event.TenantStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Should save but not log or publish event when status does not change")
        void should_save_but_not_log_or_publish_when_status_does_not_change() {
            TenantEntity entity = TenantEntity.builder().id(1L).status(TenantFileStatus.TO_PROCESS).honorDeclaration(true).build();
            Tenant tenant = new Tenant(entity);
            fr.dossierfacile.common.entity.User operator = mock(fr.dossierfacile.common.entity.User.class);

            setupToReturnStatus(TenantFileStatus.TO_PROCESS);

            var result = service.updateTenantStatus(tenant, operator);

            assertThat(result.hasBeenUpdated()).isFalse();
            assertThat(result.newStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);

            verify(jpaTenantRepository).save(tenant);
            verifyNoInteractions(addLogDomainService);
            verifyNoInteractions(eventPublisher);
        }
    }
}
