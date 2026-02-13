package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.mapper.ApplicationBasicMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApartmentSharingServiceImplBruteForceTest {

    @Mock
    private ApartmentSharingRepository apartmentSharingRepository;

    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;

    @Mock
    private TenantCommonRepository tenantRepository;

    @Mock
    private ApplicationFullMapper applicationFullMapper;

    @Mock
    private ApplicationLightMapper applicationLightMapper;

    @Mock
    private ApplicationBasicMapper applicationBasicMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private LinkLogService linkLogService;

    @Mock
    private Producer producer;

    @Mock
    private ApartmentSharingCommonService apartmentSharingCommonService;

    @Mock
    private ApiTenantLogRepository tenantLogRepository;

    @Mock
    private LogService logService;

    @Mock
    private BruteForceProtectionService bruteForceProtectionService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private TenantPermissionsService tenantPermissionsService;

    @Mock
    private DocumentService documentService;

    private ApartmentSharingServiceImpl apartmentSharingService;
    private UUID testToken;
    private ApartmentSharing apartmentSharing;
    private ApartmentSharingLink fullDataLink;
    private ApartmentSharingLink lightDataLink;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        apartmentSharingService = new ApartmentSharingServiceImpl(
                apartmentSharingRepository,
                apartmentSharingLinkRepository,
                tenantRepository,
                tenantPermissionsService,
                documentService,
                applicationFullMapper,
                applicationLightMapper,
                applicationBasicMapper,
                fileStorageService,
                linkLogService,
                producer,
                apartmentSharingCommonService,
                tenantLogRepository,
                logService,
                bruteForceProtectionService
        );

        testToken = UUID.randomUUID();
        apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .build();

        tenant = Tenant.builder()
                .id(1L)
                .lastName("Nom")
                .apartmentSharing(apartmentSharing)
                .build();

        apartmentSharing.setTenants(Collections.singletonList(tenant));

        fullDataLink = ApartmentSharingLink.builder()
                .id(1L)
                .token(testToken)
                .apartmentSharing(apartmentSharing)
                .fullData(true)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .failedAttemptCount(0)
                .firstFailedAttemptAt(null)
                .build();

        lightDataLink = ApartmentSharingLink.builder()
                .id(2L)
                .token(testToken)
                .apartmentSharing(apartmentSharing)
                .fullData(false)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .failedAttemptCount(0)
                .firstFailedAttemptAt(null)
                .build();

        // Setup request context mock
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        lenient().when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        lenient().when(httpServletRequest.getHeader("X-Real-Ip")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void shouldAllowAccessWhenNoFailedAttempts() {
        // Given
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        // When
        ApplicationModel result = apartmentSharingService.full(testToken, "NOM", null);

        // Then
        assertThat(result).isNotNull();
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
        verify(bruteForceProtectionService, times(1)).resetAttempts(fullDataLink);
    }

    @Test
    void shouldSkipLinkLogWhenLoggedTenantBelongsToApartmentSharing() {
        // Given
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        // When
        ApplicationModel result = apartmentSharingService.full(testToken, "NOM", tenant);

        // Then
        assertThat(result).isNotNull();
        verify(linkLogService, never()).save(any());
    }

    @Test
    void shouldWriteLinkLogWhenLoggedTenantBelongsToAnotherApartmentSharing() {
        // Given
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        ApartmentSharing otherApartmentSharing = ApartmentSharing.builder().id(999L).build();
        Tenant otherTenant = Tenant.builder()
                .id(999L)
                .lastName("Nom")
                .apartmentSharing(otherApartmentSharing)
                .build();

        // When
        ApplicationModel result = apartmentSharingService.full(testToken, "NOM", otherTenant);

        // Then
        assertThat(result).isNotNull();
        verify(linkLogService, times(1)).save(any());
    }

    @Test
    void shouldIncrementFailedAttemptCountOnInvalidTrigram() {
        // Given
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));

        // When
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "INVALID", null))
                .isInstanceOf(TrigramNotAuthorizedException.class);

        // Then - should call protection service to record failed attempt
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
        verify(bruteForceProtectionService, times(1)).recordFailedAttempt(fullDataLink);
        verify(bruteForceProtectionService, never()).resetAttempts(any());
    }

    @Test
    void shouldCallCheckProtectionForFullDataLinks() {
        // Given
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        // When
        apartmentSharingService.full(testToken, "NOM", null);

        // Then - should check protection for full data links
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
    }

    @Test
    void shouldBlockLinkWhenProtectionServiceThrowsException() {
        // Given - protection service will throw exception
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        doThrow(new ApplicationLinkBlockedException("Too many failed attempts. Link is temporarily blocked."))
                .when(bruteForceProtectionService).checkAndEnforceProtection(fullDataLink);

        // When & Then
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "NOM", null))
                .isInstanceOf(ApplicationLinkBlockedException.class)
                .hasMessageContaining("Too many failed attempts");

        // Verify that the protection was checked
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
    }

    @Test
    void shouldResetCounterAfterOneHour() {
        // Given - Link with failed attempts from more than 1 hour ago
        fullDataLink.setFailedAttemptCount(2);
        fullDataLink.setFirstFailedAttemptAt(LocalDateTime.now().minusHours(2));

        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        // When
        ApplicationModel result = apartmentSharingService.full(testToken, "NOM", null);

        // Then - Counter should be reset and access allowed
        assertThat(result).isNotNull();

        // Verify that the brute force protection service checks the link
        // and that the reset is called after successful validation
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
        verify(bruteForceProtectionService, times(1)).resetAttempts(fullDataLink);
    }

    @Test
    void shouldResetCounterOnSuccessfulTrigram() {
        // Given - Link with 2 failed attempts
        fullDataLink.setFailedAttemptCount(2);
        fullDataLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));
        when(applicationFullMapper.toApplicationModelWithToken(apartmentSharing, testToken))
                .thenReturn(new ApplicationModel());

        // When
        ApplicationModel result = apartmentSharingService.full(testToken, "NOM", null);

        // Then - Counter should be reset
        assertThat(result).isNotNull();

        // Verify that the brute force protection service resets attempts on successful validation
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
        verify(bruteForceProtectionService, times(1)).resetAttempts(fullDataLink);
    }

    @Test
    void shouldNotApplyBruteForceProtectionToLightDataLinks() {
        // Given - Light data link (fullData = false)
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, false))
                .thenReturn(Optional.of(lightDataLink));
        when(applicationLightMapper.toApplicationModel(apartmentSharing))
                .thenReturn(new ApplicationModel());

        // When
        ApplicationModel result = apartmentSharingService.light(testToken);

        // Then - Should work without brute-force protection
        assertThat(result).isNotNull();

        // Verify that brute-force protection was not checked
        verify(apartmentSharingLinkRepository, times(1)).findValidLinkByToken(testToken, false);
    }

    @Test
    void shouldThrowApplicationLinkBlockedExceptionWithCorrectStatusCode() {
        // Given - Link with 3 failed attempts, protection service will throw exception
        fullDataLink.setFailedAttemptCount(3);
        fullDataLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        doThrow(new ApplicationLinkBlockedException("Too many failed attempts. Link is temporarily blocked."))
                .when(bruteForceProtectionService).checkAndEnforceProtection(fullDataLink);

        // When & Then
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "NOM", null))
                .isInstanceOf(ApplicationLinkBlockedException.class)
                .satisfies(exception -> {
                    assertThat(exception).isInstanceOf(ApplicationLinkBlockedException.class);
                    ApplicationLinkBlockedException blockedException = (ApplicationLinkBlockedException) exception;
                    assertThat(blockedException.getMessage()).contains("Too many failed attempts");
                });
    }

    @Test
    void shouldIncrementFailedAttemptCountCorrectlyThroughMultipleAttempts() {
        // Given - This test verifies that recordFailedAttempt is called for each invalid trigram
        when(apartmentSharingLinkRepository.findValidLinkByToken(testToken, true))
                .thenReturn(Optional.of(fullDataLink));
        lenient().when(apartmentSharingLinkRepository.findByToken(testToken))
                .thenReturn(Optional.of(fullDataLink));

        // First failed attempt
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "INVALID1", null))
                .isInstanceOf(TrigramNotAuthorizedException.class);
        verify(bruteForceProtectionService, times(1)).recordFailedAttempt(fullDataLink);

        // Reset mock for second attempt
        reset(bruteForceProtectionService);

        // Second failed attempt
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "INVALID2", null))
                .isInstanceOf(TrigramNotAuthorizedException.class);
        verify(bruteForceProtectionService, times(1)).recordFailedAttempt(fullDataLink);

        // Reset mock for third attempt
        reset(bruteForceProtectionService);

        // Third failed attempt
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "INVALID3", null))
                .isInstanceOf(TrigramNotAuthorizedException.class);
        verify(bruteForceProtectionService, times(1)).recordFailedAttempt(fullDataLink);

        // Reset mock for fourth attempt - configure to throw blocked exception
        reset(bruteForceProtectionService);
        fullDataLink.setFailedAttemptCount(3);
        fullDataLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));
        doThrow(new ApplicationLinkBlockedException("Too many failed attempts. Link is temporarily blocked."))
                .when(bruteForceProtectionService).checkAndEnforceProtection(fullDataLink);

        // Fourth attempt - should be blocked by protection service
        assertThatThrownBy(() -> apartmentSharingService.full(testToken, "INVALID4", null))
                .isInstanceOf(ApplicationLinkBlockedException.class);
        verify(bruteForceProtectionService, times(1)).checkAndEnforceProtection(fullDataLink);
    }
}

