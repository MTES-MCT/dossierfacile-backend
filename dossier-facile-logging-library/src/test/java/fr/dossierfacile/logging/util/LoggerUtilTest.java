package fr.dossierfacile.logging.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LoggerUtilTest {


    static Stream<Arguments> provideUrisForNormalization() {
        return Stream.of(
                // Numeric ids
                Arguments.of("/api/user/123", "/api/user/{id}"),
                Arguments.of("/api/user/456/details", "/api/user/{id}/details"),
                Arguments.of("/api/orders/999/item/42", "/api/orders/{id}/item/{id}"),
                Arguments.of("/simple/path", "/simple/path"),
                Arguments.of("/user/profile/0", "/user/profile/{id}"),
                Arguments.of("/products/123/edit/456", "/products/{id}/edit/{id}"),
                // UUIDs
                Arguments.of("/preview/a79b75f3-bd1d-470e-9cd1-569672200d87", "/preview/{uuid}"),
                Arguments.of("/preview/12/file/a79b75f3-bd1d-470e-9cd1-569672200d87", "/preview/{id}/file/{uuid}"),
                Arguments.of("/preview/a79b75f3-bd1d-470e-9cd1-569672200d87/file/a79b75f3-bd1d-470e-9cd1-569672200d87", "/preview/{uuid}/file/{uuid}"),
                // UUID with trailing characters (e.g. dot, URL-encoded suffix)
                Arguments.of("/file/d3890d8a-ef89-4f38-a340-18a48272e6ea.", "/file/{uuid}"),
                Arguments.of("/file/1dcbdad6-9ccd-4a77-94da-4f27adb8d359%20J'en%20ai%20reg%C3%A9n%C3%A9r%C3%A9%20un%20nouveau", "/file/{uuid}"),
                Arguments.of("/preview/a79b75f3-bd1d-470e-9cd1-569672200d87./file/a79b75f3-bd1d-470e-9cd1-569672200d87gfgfh", "/preview/{uuid}/file/{uuid}"),
                // Emails
                Arguments.of("/email/nicolas.sagon@gmail.com", "/email/{email}"),
                Arguments.of("/email/nicolas@gmail.com.fr.de", "/email/{email}"),
                Arguments.of("/email/nicolas.sagon@gmail.com/profile", "/email/{email}/profile"),
                Arguments.of("/email/nicolas.sagon@gmail.com/profile/123/a79b75f3-bd1d-470e-9cd1-569672200d87", "/email/{email}/profile/{id}/{uuid}"),
                // Property token (20 alphanumeric chars - e.g. RandomStringUtils.randomAlphanumeric(20))
                Arguments.of("/api/property/public/vJz1UPaeeHakWs90pQVJ", "/api/property/public/{token}"),
                Arguments.of("/api/property/public/9Plc2orNTumnCjHbmpVY", "/api/property/public/{token}"),
                Arguments.of("/api/property/public/0RvF2Anw3wrvQyFj2lHi", "/api/property/public/{token}"),
                Arguments.of("/api/property/public/Abc123XyZ456QrSt7890/subscribe", "/api/property/public/{token}/subscribe"),
                // Property token + other placeholders
                Arguments.of("/api/property/public/vJz1UPaeeHakWs90pQVJ/details/42", "/api/property/public/{token}/details/{id}"),
                // Edge cases: null and empty
                Arguments.of(null, null),
                Arguments.of("", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUrisForNormalization")
    void normalizedUriParametrizedTest(String inputUri, String expectedUri) {
        String result = LoggerUtil.normalizeUrl(inputUri);
        assertThat(result).isEqualTo(expectedUri);
    }

}
