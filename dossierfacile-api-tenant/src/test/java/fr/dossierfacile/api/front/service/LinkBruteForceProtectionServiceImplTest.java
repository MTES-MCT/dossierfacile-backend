package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkBruteForceProtectionServiceImplTest {

    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;

    private LinkBruteForceProtectionServiceImpl bruteForceProtectionService;

    private ApartmentSharingLink testLink;

    @BeforeEach
    void setUp() {
        // Initialize service with default values (3 attempts, 1 hour window)
        bruteForceProtectionService = new LinkBruteForceProtectionServiceImpl(
                apartmentSharingLinkRepository,
                3, // maxFailedAttempts
                1  // timeWindowHours
        );

        // Setup test link
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

        when(apartmentSharingLinkRepository.save(any(ApartmentSharingLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAllowAccessWhenNoFailedAttemptsRecorded() {
        // Given - link with no failed attempts
        testLink.setFailedAttemptCount(0);
        testLink.setFirstFailedAttemptAt(null);

        // When & Then - should not throw exception
        bruteForceProtectionService.checkAndEnforceProtection(testLink);

        // Verify no save was called
        verify(apartmentSharingLinkRepository, never()).save(any());
    }

    @Test
    void shouldAllowAccessWithOneOrTwoFailedAttempts() {
        // Given - link with 2 failed attempts
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When & Then - should not throw exception
        bruteForceProtectionService.checkAndEnforceProtection(testLink);

        // Verify no save was called (no reset needed)
        verify(apartmentSharingLinkRepository, never()).save(any());
    }

    @Test
    void shouldBlockLinkAfterThreeFailedAttempts() {
        // Given - link with 3 failed attempts within the hour
        testLink.setFailedAttemptCount(3);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When & Then - should throw ApplicationLinkBlockedException
        assertThatThrownBy(() -> bruteForceProtectionService.checkAndEnforceProtection(testLink))
                .isInstanceOf(ApplicationLinkBlockedException.class)
                .hasMessageContaining("Too many failed attempts");

        // Verify no save was called (link is blocked)
        verify(apartmentSharingLinkRepository, never()).save(any());
    }

    @Test
    void shouldResetCounterWhenTimeWindowExpired() {
        // Given - link with failed attempts from more than 1 hour ago
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusHours(2));

        // When
        bruteForceProtectionService.checkAndEnforceProtection(testLink);

        // Then - counters should be reset
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(0);
        assertThat(savedLink.getFirstFailedAttemptAt()).isNull();
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
        // Given - link with 1 previous failed attempt
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
    void shouldResetCounterWhenRecordingAfterTimeWindowExpired() {
        // Given - link with old failed attempts
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusHours(2));

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - should reset and start new tracking
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        // First save resets, second save records new attempt
        ApartmentSharingLink finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getFailedAttemptCount()).isEqualTo(1);
        assertThat(finalSave.getFirstFailedAttemptAt()).isAfter(LocalDateTime.now().minusMinutes(1));
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
    void shouldDetectTimeWindowNotExpired() {
        // Given - recent failed attempt
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When
        boolean expired = bruteForceProtectionService.hasTimeWindowExpired(testLink);

        // Then
        assertThat(expired).isFalse();
    }

    @Test
    void shouldDetectTimeWindowExpired() {
        // Given - old failed attempt
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusHours(2));

        // When
        boolean expired = bruteForceProtectionService.hasTimeWindowExpired(testLink);

        // Then
        assertThat(expired).isTrue();
    }

    @Test
    void shouldReturnFalseForTimeWindowExpiredWhenNoFailedAttempts() {
        // Given - no failed attempts
        testLink.setFirstFailedAttemptAt(null);

        // When
        boolean expired = bruteForceProtectionService.hasTimeWindowExpired(testLink);

        // Then
        assertThat(expired).isFalse();
    }

    @Test
    void shouldBlockLinkExactlyAtThreeAttempts() {
        // Given - exactly 3 failed attempts
        testLink.setFailedAttemptCount(3);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When & Then
        assertThatThrownBy(() -> bruteForceProtectionService.checkAndEnforceProtection(testLink))
                .isInstanceOf(ApplicationLinkBlockedException.class);
    }

    @Test
    void shouldAllowAccessWithExactlyTwoAttempts() {
        // Given - exactly 2 failed attempts (just below threshold)
        testLink.setFailedAttemptCount(2);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When & Then - should not throw
        bruteForceProtectionService.checkAndEnforceProtection(testLink);
    }

    @Test
    void shouldHandleNullFailedAttemptCountGracefully() {
        // Given - null count (edge case)
        testLink.setFailedAttemptCount(null);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusMinutes(30));

        // When
        bruteForceProtectionService.recordFailedAttempt(testLink);

        // Then - should treat null as 0 and increment
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository).save(captor.capture());
        
        assertThat(captor.getValue().getFailedAttemptCount()).isEqualTo(1);
    }

    @Test
    void shouldResetWhenCheckingLinkBlockedForMoreThanTimeWindow() {
        // Given - link blocked but time window has expired
        testLink.setFailedAttemptCount(5);
        testLink.setFirstFailedAttemptAt(LocalDateTime.now().minusHours(2));

        // When
        bruteForceProtectionService.checkAndEnforceProtection(testLink);

        // Then - should reset instead of blocking
        ArgumentCaptor<ApartmentSharingLink> captor = ArgumentCaptor.forClass(ApartmentSharingLink.class);
        verify(apartmentSharingLinkRepository, times(1)).save(captor.capture());

        ApartmentSharingLink savedLink = captor.getValue();
        assertThat(savedLink.getFailedAttemptCount()).isEqualTo(0);
        assertThat(savedLink.getFirstFailedAttemptAt()).isNull();
    }
}



