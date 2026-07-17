package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void should_return_user_preferred_name_when_owner_type_is_self() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.SELF).build();
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isEqualTo("Owner Preferred Name");
    }

    @Test
    void should_return_tenant_preferred_name_when_owner_type_is_third_party_and_tenant_preferred_name_is_set() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();
        tenant.setPreferredName("Third Party Preferred Name");
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isEqualTo("Third Party Preferred Name");
    }

    @Test
    void should_return_null_when_owner_type_is_third_party_and_tenant_preferred_name_is_missing() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isNull();
    }

    @Test
    void should_return_null_when_owner_type_is_third_party_and_no_preferred_name_is_set() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();

        assertThat(tenant.getPreferredName()).isNull();
    }

    @Nested
    @DisplayName("Tests for computeStatus() and isAllCategories()")
    class ComputeStatusTest {

        private Document buildDocument(DocumentCategory category, DocumentStatus status) {
            return Document.builder()
                    .documentCategory(category)
                    .documentStatus(status)
                    .build();
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

        @Test
        @DisplayName("Should return ARCHIVED when status is ARCHIVED")
        void should_return_archived_when_status_is_archived() {
            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.ARCHIVED)
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should return DECLINED when any document is DECLINED")
        void should_return_declined_when_any_document_is_declined() {
            List<Document> docs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            docs.add(buildDocument(DocumentCategory.FINANCIAL, DocumentStatus.DECLINED));

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(docs)
                    .guarantors(List.of())
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.DECLINED);
        }

        @Test
        @DisplayName("Should return DECLINED when guarantor document is DECLINED")
        void should_return_declined_when_guarantor_document_is_declined() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildDocument(DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE, DocumentStatus.DECLINED));

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.ORGANISM)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.DECLINED);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when honorDeclaration is false")
        void should_return_incomplete_when_honor_declaration_is_false() {
            List<Document> docs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(false)
                    .documents(docs)
                    .guarantors(List.of())
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when mandatory categories are missing on tenant")
        void should_return_incomplete_when_tenant_missing_categories() {
            List<Document> incompleteDocs = List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.RESIDENCY, DocumentStatus.VALIDATED)
            );

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(incompleteDocs)
                    .guarantors(List.of())
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor ORGANISM does not have exact certificate")
        void should_return_incomplete_when_organism_guarantor_missing_certificate() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.ORGANISM)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor NATURAL_PERSON is missing mandatory categories")
        void should_return_incomplete_when_natural_person_guarantor_missing_categories() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return INCOMPLETE when guarantor LEGAL_PERSON is missing mandatory categories")
        void should_return_incomplete_when_legal_person_guarantor_missing_categories() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED));

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("Should return TO_PROCESS when honorDeclaration is true, categories present, and any document is TO_PROCESS")
        void should_return_to_process_when_document_is_to_process() {
            List<Document> tenantDocs = new ArrayList<>(List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.TO_PROCESS),
                    buildDocument(DocumentCategory.RESIDENCY, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.PROFESSIONAL, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.FINANCIAL, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.TAX, DocumentStatus.VALIDATED)
            ));

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of())
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
        }

        @Test
        @DisplayName("Should return VALIDATED when honorDeclaration is true, categories present, and all documents are VALIDATED")
        void should_return_validated_when_all_conditions_met_without_guarantor() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of())
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.VALIDATED);
        }

        @Test
        @DisplayName("Should return VALIDATED when tenant and NATURAL_PERSON guarantor are fully complete and VALIDATED")
        void should_return_validated_with_natural_person_guarantor() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.VALIDATED);
        }

        @Test
        @DisplayName("Should return VALIDATED when tenant and LEGAL_PERSON guarantor are fully complete and VALIDATED")
        void should_return_validated_with_legal_person_guarantor() {
            List<Document> tenantDocs = buildTenantMandatoryDocuments(DocumentStatus.VALIDATED);
            List<Document> guarantorDocs = List.of(
                    buildDocument(DocumentCategory.IDENTIFICATION, DocumentStatus.VALIDATED),
                    buildDocument(DocumentCategory.IDENTIFICATION_LEGAL_PERSON, DocumentStatus.VALIDATED)
            );

            Guarantor guarantor = Guarantor.builder()
                    .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                    .documents(guarantorDocs)
                    .build();

            Tenant tenant = Tenant.builder()
                    .status(TenantFileStatus.TO_PROCESS)
                    .honorDeclaration(true)
                    .documents(tenantDocs)
                    .guarantors(List.of(guarantor))
                    .build();

            assertThat(tenant.computeStatus()).isEqualTo(TenantFileStatus.VALIDATED);
        }
    }
}
