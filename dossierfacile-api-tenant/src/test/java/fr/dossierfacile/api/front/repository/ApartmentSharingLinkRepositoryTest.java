package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaTestApplication.class)
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
class ApartmentSharingLinkRepositoryTest {

    @Autowired
    private ApartmentSharingLinkRepository repository;

    @Autowired
    private TestEntityManager em;

    private UUID validToken;
    private UUID disabledToken;
    private UUID deletedToken;
    private UUID expiredToken;
    private UUID futureToken;

    @BeforeEach
    void setUp() {
        ApartmentSharing sharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(sharing);

        validToken = UUID.randomUUID();
        disabledToken = UUID.randomUUID();
        deletedToken = UUID.randomUUID();
        expiredToken = UUID.randomUUID();
        futureToken = UUID.randomUUID();

        // Valid link: not disabled, not deleted, no expiration
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(validToken)
                .fullData(false)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        // Disabled link
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(disabledToken)
                .fullData(false)
                .disabled(true)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        // Deleted link
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(deletedToken)
                .fullData(false)
                .disabled(false)
                .deleted(true)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        // Expired link (expiration date in the past)
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(expiredToken)
                .fullData(false)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .expirationDate(LocalDateTime.of(2020, 1, 1, 0, 0))
                .build());

        // Link with future expiration
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .token(futureToken)
                .fullData(false)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .expirationDate(LocalDateTime.of(2099, 1, 1, 0, 0))
                .build());

        em.flush();
    }

    @Test
    void shouldFindValidLink() {
        Optional<ApartmentSharingLink> result = repository.findValidLinkByToken(validToken, false);
        assertThat(result).isPresent();
    }

    @Test
    void shouldNotFindDisabledLink() {
        Optional<ApartmentSharingLink> result = repository.findValidLinkByToken(disabledToken, false);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotFindDeletedLink() {
        Optional<ApartmentSharingLink> result = repository.findValidLinkByToken(deletedToken, false);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotFindExpiredLink() {
        Optional<ApartmentSharingLink> result = repository.findValidLinkByToken(expiredToken, false);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindLinkWithFutureExpiration() {
        Optional<ApartmentSharingLink> result = repository.findValidLinkByToken(futureToken, false);
        assertThat(result).isPresent();
    }
}