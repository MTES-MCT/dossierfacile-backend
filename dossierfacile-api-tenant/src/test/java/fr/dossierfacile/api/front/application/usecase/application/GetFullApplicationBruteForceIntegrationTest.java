package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.projection.FullApplicationResponseProjection;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase.GetFullApplicationCommand;
import fr.dossierfacile.api.front.domain.policy.LinkBruteForcePolicy;
import fr.dossierfacile.api.front.domain.policy.TrigramAccessPolicy;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.fixtures.ApplicationSeed;
import fr.dossierfacile.api.front.fixtures.ApplicationSeed.Seed;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.api.front.service.LinkBruteForceProtectionServiceImpl;
import fr.dossierfacile.common.domain.service.ApartmentSharingStatusDomainService;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the brute-force durability invariant with the real protection service and a real
 * transaction manager: the failed-attempt counter must be committed (REQUIRES_NEW) even
 * though the 403 rolls the use case transaction back. A mocked-transaction unit test
 * cannot catch a regression here (e.g. someone removing REQUIRES_NEW).
 */
@SpringBootTest
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({
        JpaApartmentSharingRepository.class,
        JpaTenantRepository.class,
        JpaGuarantorRepository.class,
        JpaDocumentRepository.class,
        ApartmentSharingStatusDomainService.class,
        ApplicationProjectionLoader.class,
        FullApplicationResponseProjection.class,
        TrigramAccessPolicy.class,
        LinkBruteForcePolicy.class,
        LinkBruteForceProtectionServiceImpl.class,
        GetFullApplicationUseCase.class
})
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:brutedb;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=update",
        "application.base.url=http://api.test",
        "tenant.base.url=http://front.test"
})
class GetFullApplicationBruteForceIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GetFullApplicationUseCase useCase;

    @MockitoBean
    private LinkLogService linkLogService;

    private TransactionTemplate tx;
    private Seed seed;

    @Autowired
    void initTransactionTemplate(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        seed = tx.execute(status -> ApplicationSeed.seed(em));
    }

    @Test
    void failed_attempt_counter_is_committed_despite_the_403_rollback() {
        assertThatThrownBy(() -> useCase.execute(command("BAD")))
                .isInstanceOf(TrigramNotAuthorizedException.class);

        ApartmentSharingLink link = findLinkByToken(seed.validToken());
        assertThat(link.getFailedAttemptCount()).isEqualTo(1);
        assertThat(link.getFirstFailedAttemptAt()).isNotNull();
    }

    @Test
    void counter_accumulates_over_failed_attempts_and_resets_on_success() {
        assertThatThrownBy(() -> useCase.execute(command("BAD")))
                .isInstanceOf(TrigramNotAuthorizedException.class);
        assertThatThrownBy(() -> useCase.execute(command("BAD")))
                .isInstanceOf(TrigramNotAuthorizedException.class);

        assertThat(findLinkByToken(seed.validToken()).getFailedAttemptCount()).isEqualTo(2);

        useCase.execute(command(ApplicationSeed.MAIN_TENANT_TRIGRAM));

        ApartmentSharingLink link = findLinkByToken(seed.validToken());
        assertThat(link.getFailedAttemptCount()).isZero();
        assertThat(link.getFirstFailedAttemptAt()).isNull();
    }

    @Test
    void stale_counters_restart_from_one_after_the_time_window_expired() {
        // Stale state: 2 failed attempts recorded 2 hours ago (window is 1 hour)
        tx.executeWithoutResult(status -> em.createQuery(
                        "update ApartmentSharingLink l set l.failedAttemptCount = 2, l.firstFailedAttemptAt = :first where l.token = :token")
                .setParameter("first", java.time.LocalDateTime.now().minusHours(2))
                .setParameter("token", seed.validToken())
                .executeUpdate());

        assertThatThrownBy(() -> useCase.execute(command("BAD")))
                .isInstanceOf(TrigramNotAuthorizedException.class);

        // The stale counter is reinitialized, not incremented: fresh window, count restarts at 1
        ApartmentSharingLink link = findLinkByToken(seed.validToken());
        assertThat(link.getFailedAttemptCount()).isEqualTo(1);
        assertThat(link.getFirstFailedAttemptAt()).isAfter(java.time.LocalDateTime.now().minusMinutes(5));
    }

    private GetFullApplicationCommand command(String trigram) {
        return new GetFullApplicationCommand(seed.validToken(), trigram, null, "127.0.0.1");
    }

    private ApartmentSharingLink findLinkByToken(UUID token) {
        return tx.execute(status -> em.createQuery(
                        "select l from ApartmentSharingLink l where l.token = :token", ApartmentSharingLink.class)
                .setParameter("token", token)
                .getSingleResult());
    }
}
