package fr.gouv.bo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserPrincipalSerializationTest {

    @Test
    void shouldKeepOidcIdTokenAfterSerialization() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "John",
                "john.doe@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );

        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "john-doe")
        );
        principal.setOidcData(idToken, null, null);

        byte[] serialized = serialize(principal);
        UserPrincipal deserialized = deserialize(serialized);

        assertNotNull(deserialized.getIdToken());
        assertEquals("id-token-value", deserialized.getIdToken().getTokenValue());
    }

    private static byte[] serialize(UserPrincipal principal) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(principal);
        }
        return outputStream.toByteArray();
    }

    private static UserPrincipal deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (UserPrincipal) objectInputStream.readObject();
        }
    }
}

