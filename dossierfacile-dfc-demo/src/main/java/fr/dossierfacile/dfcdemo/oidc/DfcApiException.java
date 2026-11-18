package fr.dossierfacile.dfcdemo.oidc;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Raised when one step of the DossierFacile Connect flow fails. Carries the raw upstream answer so the demo can
 * show partners exactly what Keycloak or the tenant API returned.
 */
@Getter
public class DfcApiException extends RuntimeException {

    private final String step;
    private final HttpStatusCode status;
    private final String responseBody;

    public DfcApiException(String step, HttpStatusCode status, String responseBody) {
        super(step + " failed" + (status == null ? "" : " with status " + status));
        this.step = step;
        this.status = status;
        this.responseBody = responseBody;
    }
}
