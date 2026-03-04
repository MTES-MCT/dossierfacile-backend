package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaTestApplication.class)
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private TestEntityManager em;

    private ApartmentSharing sharing1;
    private String tenantDocName;
    private String coTenantDocName;
    private String guarantorDocName;
    private String otherDocName;

    @BeforeEach
    void setUp() {
        // Apartment sharing 1: main tenant + co-tenant + guarantor
        sharing1 = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .build();
        em.persist(sharing1);

        Tenant mainTenant = Tenant.builder()
                .email("main@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.CREATE)
                .build();
        em.persist(mainTenant);

        Tenant coTenant = Tenant.builder()
                .email("co@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.JOIN)
                .build();
        em.persist(coTenant);

        Guarantor guarantor = Guarantor.builder()
                .firstName("Guarantor")
                .lastName("One")
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(mainTenant)
                .build();
        em.persist(guarantor);

        // Documents
        tenantDocName = "tenant-doc";
        Document tenantDoc = Document.builder()
                .name(tenantDocName)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .tenant(mainTenant)
                .build();
        em.persist(tenantDoc);

        coTenantDocName = "cotenant-doc";
        Document coTenantDoc = Document.builder()
                .name(coTenantDocName)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .tenant(coTenant)
                .build();
        em.persist(coTenantDoc);

        guarantorDocName = "guarantor-doc";
        Document guarantorDoc = Document.builder()
                .name(guarantorDocName)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .guarantor(guarantor)
                .build();
        em.persist(guarantorDoc);

        // Apartment sharing 2: isolated tenant (should not be found)
        ApartmentSharing sharing2 = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(sharing2);

        Tenant otherTenant = Tenant.builder()
                .email("other@test.com")
                .apartmentSharing(sharing2)
                .tenantType(TenantType.CREATE)
                .build();
        em.persist(otherTenant);

        otherDocName = "other-doc";
        Document otherDoc = Document.builder()
                .name(otherDocName)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .tenant(otherTenant)
                .build();
        em.persist(otherDoc);

        em.flush();
    }

    @Test
    void shouldFindTenantDocument() {
        Optional<Document> result = repository.findByNameForApartmentSharing(tenantDocName, sharing1.getId());
        assertThat(result).isPresent();
    }

    @Test
    void shouldFindCoTenantDocument() {
        Optional<Document> result = repository.findByNameForApartmentSharing(coTenantDocName, sharing1.getId());
        assertThat(result).isPresent();
    }

    @Test
    void shouldFindGuarantorDocument() {
        Optional<Document> result = repository.findByNameForApartmentSharing(guarantorDocName, sharing1.getId());
        assertThat(result).isPresent();
    }

    @Test
    void shouldNotFindDocumentFromOtherSharing() {
        Optional<Document> result = repository.findByNameForApartmentSharing(otherDocName, sharing1.getId());
        assertThat(result).isEmpty();
    }
}