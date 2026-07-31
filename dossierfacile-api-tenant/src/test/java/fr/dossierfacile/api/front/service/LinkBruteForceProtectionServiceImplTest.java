package fr.dossierfacile.api.front.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Write side only (1 hour window): the blocked-or-not decision is covered by LinkBruteForcePolicyTest.
 */
@ExtendWith(MockitoExtension.class)
class LinkBruteForceProtectionServiceImplTest {

    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;

    private LinkBruteForceProtectionServiceImpl bruteForceProtectionService;

    private ApartmentSharingLink testLink;

    @BeforeEach
    void setUp() {
        bruteForceProtectionService = new LinkBruteForceProtectionServiceImpl(
                apartmentSharingLinkRepository,
                1 // timeWindowHours
        );

        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .build();

        testLink = ApartmentSharingLink.builder()
                .id(1L)
                .token(UUID.randomUUID())
                .apartmentSharing(apartmentSharing)
                .fullData(true)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .failedAttemptCount(0)
                .firstFailedAttemptAt(null)
                .build();

        lenient().when(apartmentSharingLinkRepository.save(any(ApartmentSharingLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRecordFailedAttemptAndInitializeTracking() {
        // Given - link with no previous attempts
        testLink.setFailedAttemptCount(0);
        testLink.setFirstFailedAttemptAt(null);

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - should initialize tracking
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(1);
        assertThat(savedLink.getFirstFailedAttemptAt()).isNotNull();
        assertThat(savedLink.getFirstFailedAttemptAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldIncrementFailedAttemptCountOnSubsequentFailures() {
        // Given - link with 1 previous failed attempt within the window
        LocalDateTime firstAttempt = LocalDateTime.now().minusMinutes(10);
        testLink.setFailedAttemptCount(1);
        testLink.setFirstFailedAttemptAt(firstAttempt);

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - should increment counter but keep first attempt time
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(2);
        assertThat(savedLink.getFirstFailedAttemptAt()).isEqualTo(firstAttempt);
    }

    @Test
    void shouldReinitializeTrackingWhenRecordingAfterTimeWindowExpired() {
        // Given - stale attempts (expired window): the pure policy no longer resets them,
        // so recording must restart the tracking instead of incrementing a stale counter
        LocalDateTime oldFirstAttempt = LocalDateTime.now().minusHours(2);
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(oldFirstAttempt);

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - fresh window, count restarts at 1
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(1);
        assertThat(savedLink.getFirstFailedAttemptAt()).isAfter(oldFirstAttempt);
    }

    @Test
    void shouldResetAttemptsCorrectly() {
        // Given - link with failed attempts
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When
        bruteForceProtectionService.resetAttempts(testLink);

        // Then
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(0);
        assertThat(savedLink.getFirstFailedAttemptAt()).isNull();
    }

    @Test
    void shouldSkipResetWhenAlreadyReset() {
        // Given - link with no counters at all
        testLink.setFailedAttemptCount(null);
        testLink.setFirstFailedAttemptAt(null);

        // When
        bruteForceProtectionService.resetAttempts(testLink);

        // Then - no useless write
        verify(apartmentSharingLinkRepository, never()).save(any());
    }

    @Test
    void shouldHandleNullFailedAttemptCountGracefully() {
        // Given - null count within an active window (edge case)
        testLink.setFailedAttemptCount(null);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - should treat null as 0 and increment
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository).save(captor.capture());

        assertThat(captor.getValue().getFailedAttemptCount()).isEqualTo(1);
    }
}
