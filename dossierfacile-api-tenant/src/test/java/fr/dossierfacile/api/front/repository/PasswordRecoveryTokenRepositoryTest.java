package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = PasswordRecoveryTokenRepositoryTest.JpaConfiguration.class)
class PasswordRecoveryTokenRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = PasswordRecoveryTokenRepository.class)
    static class JpaConfiguration {
    }

    @Autowired
    private PasswordRecoveryTokenRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldResolveOnlyTenantTokensAndPreserveForeignTokens() {
        var tenant = entityManager.persist(Tenant.builder().email("tenant@example.com").build());
        var owner = entityManager.persist(Owner.builder().email("owner@example.com").build());
        var tenantToken = entityManager.persist(PasswordRecoveryToken.builder().token("tenant-token").user(tenant).build());
        var ownerToken = entityManager.persist(PasswordRecoveryToken.builder().token("owner-token").user(owner).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findTenantTokenByToken("tenant-token"))
                .isPresent()
                .get().extracting(PasswordRecoveryToken::getId).isEqualTo(tenantToken.getId());
        assertThat(repository.findTenantTokenByToken("owner-token")).isEmpty();
        assertThat(repository.findTenantTokenByToken("unknown-token")).isEmpty();
        assertThat(repository.findById(ownerToken.getId())).isPresent();
    }
}
