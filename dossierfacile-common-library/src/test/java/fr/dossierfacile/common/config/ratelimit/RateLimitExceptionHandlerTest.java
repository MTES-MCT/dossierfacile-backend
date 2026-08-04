package fr.dossierfacile.common.config.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceptionHandlerTest {

    private final RateLimitExceptionHandler handler = new RateLimitExceptionHandler();

    @Test
    void handleRateLimitExceeded_returnsHttpStatus429TooManyRequests() {
        RateLimitExceededException exception = new RateLimitExceededException("Too Many Requests");

        ResponseEntity<String> response = handler.handleRateLimitExceeded(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.getBody()).isEqualTo("Too Many Requests");
    }
}
