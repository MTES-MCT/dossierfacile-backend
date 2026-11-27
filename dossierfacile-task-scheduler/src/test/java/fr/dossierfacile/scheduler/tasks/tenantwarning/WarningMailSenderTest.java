package fr.dossierfacile.scheduler.tasks.tenantwarning;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarningMailSenderTest {

    private final TransactionalEmailsApi brevo = mock(TransactionalEmailsApi.class);
    private final WarningMailSender sender = new WarningMailSender(brevo);
    private final ConfirmationToken token = mock(ConfirmationToken.class);
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(42L);
        tenant.setEmail("tenant@example.com");
        when(token.getToken()).thenReturn("token");
    }

    @Test
    void should_propagate_a_timeout_so_that_the_warning_is_retried() throws ApiException {
        // The Brevo SDK wraps network errors in an ApiException with httpCode 0
        when(brevo.sendTransacEmail(any())).thenThrow(new ApiException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> sender.sendEmailFirstWarningForDeletionOfDocuments(tenant, token))
                .isInstanceOf(TransientMailDeliveryException.class)
                .hasMessageContaining("tenant 42")
                .hasMessageContaining("SocketTimeoutException: timeout");
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void should_propagate_rate_limiting_and_server_errors(int httpCode) throws ApiException {
        when(brevo.sendTransacEmail(any())).thenThrow(new ApiException(httpCode, "error"));

        assertThatThrownBy(() -> sender.sendEmailSecondWarningForDeletionOfDocuments(tenant, token))
                .isInstanceOf(TransientMailDeliveryException.class)
                .satisfies(e -> assertThat(((TransientMailDeliveryException) e).getHttpCode()).isEqualTo(httpCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404})
    void should_not_retry_permanent_errors(int httpCode) throws ApiException {
        when(brevo.sendTransacEmail(any())).thenThrow(new ApiException(httpCode, "error"));

        assertThatCode(() -> sender.sendEmailFirstWarningForDeletionOfDocuments(tenant, token))
                .doesNotThrowAnyException();
    }

    @Test
    void should_keep_owner_warnings_best_effort() throws ApiException {
        Owner owner = new Owner();
        owner.setId(7L);
        owner.setEmail("owner@example.com");
        when(brevo.sendTransacEmail(any())).thenThrow(new ApiException(new SocketTimeoutException("timeout")));

        assertThatCode(() -> sender.sendEmailFirstWarningForDeletionOfOwner(owner, token))
                .doesNotThrowAnyException();
    }
}
