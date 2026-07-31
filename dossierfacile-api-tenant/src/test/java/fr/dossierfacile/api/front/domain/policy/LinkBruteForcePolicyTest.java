package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.exception.ApplicationLinkBlockedException;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure policy: 3 attempts max, 1 hour window. No mock needed.
 */
class LinkBruteForcePolicyTest {

    private final LinkBruteForcePolicy policy = new LinkBruteForcePolicy(3, 1);

    private ApartmentSharingLink link(Integer failedAttemptCount, LocalDateTime firstFailedAttemptAt) {
        return ApartmentSharingLink.builder()
                .id(1L)
                .token(UUID.randomUUID())
                .failedAttemptCount(failedAttemptCount)
                .firstFailedAttemptAt(firstFailedAttemptAt)
                .build();
    }

    @Test
    void allows_access_when_no_failed_attempts_recorded() {
        assertThatCode(() -> policy.checkNotBlocked(link(0, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_access_below_the_threshold() {
        assertThatCode(() -> policy.checkNotBlocked(link(2, LocalDateTime.now().minusMinutes(30))))
                .doesNotThrowAnyException();
    }

    @Test
    void blocks_at_the_threshold() {
        assertThatThrownBy(() -> policy.checkNotBlocked(link(3, LocalDateTime.now().minusMinutes(30))))
                .isInstanceOf(ApplicationLinkBlockedException.class)
                .hasMessageContaining("Too many failed attempts");
    }

    @Test
    void blocks_above_the_threshold() {
        assertThatThrownBy(() -> policy.checkNotBlocked(link(5, LocalDateTime.now().minusMinutes(30))))
                .isInstanceOf(ApplicationLinkBlockedException.class);
    }

    @Test
    void allows_access_again_when_time_window_expired() {
        // Stale counters do not block; they are reinitialized by the next write, not here
        assertThatCode(() -> policy.checkNotBlocked(link(5, LocalDateTime.now().minusHours(2))))
                .doesNotThrowAnyException();
    }

    @Test
    void handles_null_count_gracefully() {
        assertThatCode(() -> policy.checkNotBlocked(link(null, LocalDateTime.now().minusMinutes(30))))
                .doesNotThrowAnyException();
    }
}
