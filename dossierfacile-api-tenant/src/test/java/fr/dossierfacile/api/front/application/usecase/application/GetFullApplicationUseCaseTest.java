package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionSources;
import fr.dossierfacile.api.front.application.projection.FullApplicationResponseProjection;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase.GetFullApplicationCommand;
import fr.dossierfacile.api.front.domain.policy.LinkBruteForcePolicy;
import fr.dossierfacile.api.front.domain.policy.TrigramAccessPolicy;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetFullApplicationUseCaseTest {

    private static final UUID TOKEN = UUID.fromString("186ed480-968b-4bcd-aaab-afa747b953da");
    private static final long SHARING_ID = 42L;

    private GetFullApplicationUseCase useCase;

    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;
    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    @Mock
    private LinkBruteForcePolicy linkBruteForcePolicy;
    @Mock
    private BruteForceProtectionService bruteForceProtectionService;
    @Mock
    private TrigramAccessPolicy trigramAccessPolicy;
    @Mock
    private ApplicationProjectionLoader applicationProjectionLoader;
    @Mock
    private FullApplicationResponseProjection fullApplicationResponseProjection;
    @Mock
    private LinkLogService linkLogService;
    @Mock
    private JpaTenantRepository jpaTenantRepository;

    private ApartmentSharingLink link;
    private ApplicationProjectionSources sources;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        useCase = new GetFullApplicationUseCase(
                transactionManager,
                apartmentSharingLinkRepository,
                linkBruteForcePolicy,
                bruteForceProtectionService,
                trigramAccessPolicy,
                applicationProjectionLoader,
                fullApplicationResponseProjection,
                linkLogService,
                jpaTenantRepository
        );

        ApartmentSharing sharing = ApartmentSharing.builder().id(SHARING_ID).build();
        link = ApartmentSharingLink.builder().apartmentSharing(sharing).token(TOKEN).fullData(true).build();
        sources = new ApplicationProjectionSources(null, List.of(), Map.of(), Map.of(), Map.of(), Map.of(), null, null);

        lenient().when(apartmentSharingLinkRepository.findValidLinkByToken(TOKEN, true)).thenReturn(Optional.of(link));
        lenient().when(applicationProjectionLoader.load(SHARING_ID)).thenReturn(sources);
        lenient().when(fullApplicationResponseProjection.project(sources, TOKEN)).thenReturn(new ApplicationModel());
    }

    private GetFullApplicationCommand command(String trigram, String viewerKeycloakId) {
        return new GetFullApplicationCommand(TOKEN, trigram, viewerKeycloakId, "127.0.0.1");
    }

    private void givenViewerBelongsToSharing(String keycloakId, Long apartmentSharingId) {
        Tenant viewer = new Tenant(TenantEntity.builder().id(7L).apartmentSharingId(apartmentSharingId).build());
        when(jpaTenantRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(viewer));
    }

    @Test
    void throws_404_when_link_does_not_exist() {
        when(apartmentSharingLinkRepository.findValidLinkByToken(TOKEN, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command("DUP", null)))
                .isInstanceOf(ApartmentSharingNotFoundException.class);
    }

    @Test
    void throws_429_and_stops_when_link_is_blocked() {
        doThrow(new ApplicationLinkBlockedException("blocked"))
                .when(linkBruteForcePolicy).checkNotBlocked(link);

        assertThatThrownBy(() -> useCase.execute(command("DUP", null)))
                .isInstanceOf(ApplicationLinkBlockedException.class);

        verify(applicationProjectionLoader, never()).load(any());
        verify(linkLogService, never()).save(any(LinkLog.class));
    }

    @Test
    void records_failed_attempt_before_propagating_403_on_invalid_trigram() {
        doThrow(new TrigramNotAuthorizedException("no match"))
                .when(trigramAccessPolicy).validateAccess("BAD", sources.tenants());

        assertThatThrownBy(() -> useCase.execute(command("BAD", null)))
                .isInstanceOf(TrigramNotAuthorizedException.class);

        InOrder inOrder = inOrder(trigramAccessPolicy, bruteForceProtectionService);
        inOrder.verify(trigramAccessPolicy).validateAccess("BAD", sources.tenants());
        inOrder.verify(bruteForceProtectionService).recordFailedAttempt(link);
        verify(bruteForceProtectionService, never()).resetAttempts(any());
        verify(linkLogService, never()).save(any(LinkLog.class));
    }

    @Test
    void resets_attempts_and_logs_consultation_on_success() {
        ApplicationModel model = useCase.execute(command("DUP", null));

        assertThat(model).isNotNull();
        verify(bruteForceProtectionService).resetAttempts(link);
        verify(linkLogService).save(any(LinkLog.class));
        verify(fullApplicationResponseProjection).project(sources, TOKEN);
    }

    @Test
    void skips_link_log_when_logged_tenant_consults_their_own_application() {
        givenViewerBelongsToSharing("kc-viewer", SHARING_ID);

        useCase.execute(command("DUP", "kc-viewer"));

        verify(linkLogService, never()).save(any(LinkLog.class));
        verify(bruteForceProtectionService).resetAttempts(link);
    }

    @Test
    void logs_consultation_when_logged_tenant_belongs_to_another_application() {
        givenViewerBelongsToSharing("kc-viewer", 999L);

        useCase.execute(command("DUP", "kc-viewer"));

        verify(linkLogService).save(any(LinkLog.class));
    }

    @Test
    void logs_consultation_when_viewer_keycloak_id_is_unknown() {
        when(jpaTenantRepository.findByKeycloakId("kc-ghost")).thenReturn(Optional.empty());

        useCase.execute(command("DUP", "kc-ghost"));

        verify(linkLogService).save(any(LinkLog.class));
    }
}
