package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:tenant-email-search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TenantEmailSearchTest.JpaConfiguration.class)
class TenantEmailSearchTest {

    @Configuration
    @EntityScan(basePackageClasses = Tenant.class)
    @EnableJpaRepositories(basePackageClasses = TenantCommonRepository.class)
    static class JpaConfiguration {
    }

    @Autowired
    private TenantCommonRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsAloneApplicationByGuarantorEmailIgnoringCase() {
        Tenant tenant = persistTenant("tenant@example.com");
        persistGuarantor(tenant, "Guarantor+Test@example.com");
        entityManager.flush();
        entityManager.clear();

        var results = repository.findByTenantOrGuarantorEmailIgnoreCase(
                "GUARANTOR+TEST@EXAMPLE.COM", page(0, 20));

        assertThat(results.getContent()).extracting(Tenant::getId).containsExactly(tenant.getId());
        assertThat(results.getContent().getFirst().getGuarantors())
                .extracting(Guarantor::getEmail).containsExactly("Guarantor+Test@example.com");
    }

    @Test
    void includesTenantAndGuarantorMatchesOnceAndPaginatesWithTheCorrectTotal() {
        Tenant first = persistTenant("shared@example.com");
        Tenant second = persistTenant("second@example.com");
        persistGuarantor(second, "shared@example.com");
        persistGuarantor(second, "SHARED@example.com");
        Tenant third = persistTenant("third@example.com");
        persistGuarantor(third, "shared@example.com");
        persistTenant("unrelated@example.com");
        entityManager.flush();
        entityManager.clear();

        var firstPage = repository.findByTenantOrGuarantorEmailIgnoreCase("shared@example.com", page(0, 2));
        var secondPage = repository.findByTenantOrGuarantorEmailIgnoreCase("shared@example.com", page(1, 2));

        assertThat(firstPage.getContent()).extracting(Tenant::getId).containsExactly(third.getId(), second.getId());
        assertThat(secondPage.getContent()).extracting(Tenant::getId).containsExactly(first.getId());
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private Tenant persistTenant(String email) {
        ApartmentSharing sharing = entityManager.persist(ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build());
        return entityManager.persist(Tenant.builder().email(email).apartmentSharing(sharing).build());
    }

    private void persistGuarantor(Tenant tenant, String email) {
        entityManager.persist(Guarantor.builder()
                .tenant(tenant)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .email(email)
                .build());
    }

    private PageRequest page(int number, int size) {
        return PageRequest.of(number, size, Sort.by("id").descending());
    }
}
