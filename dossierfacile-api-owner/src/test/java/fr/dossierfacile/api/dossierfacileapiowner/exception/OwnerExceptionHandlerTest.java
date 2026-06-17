package fr.dossierfacile.api.dossierfacileapiowner.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnerExceptionHandlerTest {

    private final OwnerExceptionHandler handler = new OwnerExceptionHandler();

    @Test
    void shouldMapDpeNotFoundToBadRequest() {
        var exception = new OwnerApiException(
                OwnerApiErrorCode.DPE_NOT_FOUND,
                "Le DPE 2178V1001934U est introuvable.",
                Map.of("dpeNumber", "2178V1001934U")
        );

        var response = handler.handleOwnerApiException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DPE_NOT_FOUND", response.getBody().code());
        assertEquals("2178V1001934U", response.getBody().details().get("dpeNumber"));
    }

    @Test
    void shouldMapAdemeUnavailableToBadGateway() {
        var exception = new OwnerApiException(OwnerApiErrorCode.ADEME_UNAVAILABLE, "ADEME service unavailable");

        var response = handler.handleOwnerApiException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("ADEME_UNAVAILABLE", response.getBody().code());
    }
}
