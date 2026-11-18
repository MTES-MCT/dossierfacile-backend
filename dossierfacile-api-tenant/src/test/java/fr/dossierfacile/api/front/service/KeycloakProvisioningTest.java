package fr.dossierfacile.api.front.service;

import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakProvisioningTest {
    private static final String EMAIL = "invitee@example.com";
    private final RealmResource realm = mock(RealmResource.class);
    private final UsersResource users = mock(UsersResource.class);
    private final UserResource resource = mock(UserResource.class);
    private final Response response = mock(Response.class);

    private final KeycloakServiceImpl service = new KeycloakServiceImpl(realm, mock(KeycloakCommonService.class));

    @BeforeEach
    void setUp() {
        when(realm.users()).thenReturn(users);
    }

    @Test
    void shouldResolveExactEmailInsteadOfFirstPartialResultOnConflict() {
        createResponse(409);
        var exact = identity("exact-id", "INVITEE@example.com");
        when(users.searchByEmail(EMAIL, true)).thenReturn(Arrays.asList(
                identity("wrong-id", "invitee@example.com.evil"), null, exact));
        when(users.get("exact-id")).thenReturn(resource);
        when(resource.toRepresentation()).thenReturn(exact);

        assertEquals("exact-id", service.createKeycloakUser(EMAIL));

        verify(users).searchByEmail(EMAIL, true);
        verify(users, never()).search(anyString());
        verify(users, never()).get("wrong-id");
        verify(resource, never()).update(any());
        verify(response).close();
    }

    static Stream<Arguments> ambiguousResults() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(List.of()),
                Arguments.of(Arrays.asList((UserRepresentation) null)),
                Arguments.of(List.of(identity("wrong", "invitee@example.com.evil"))),
                Arguments.of(List.of(identity("first", EMAIL), identity("second", "INVITEE@EXAMPLE.COM"))),
                Arguments.of(List.of(identity(null, EMAIL))),
                Arguments.of(List.of(identity(" ", EMAIL))));
    }

    @ParameterizedTest
    @MethodSource("ambiguousResults")
    void shouldRejectMissingOrAmbiguousExactMatch(List<UserRepresentation> results) {
        createResponse(409);
        when(users.searchByEmail(EMAIL, true)).thenReturn(results);

        assertThrows(IllegalStateException.class, () -> service.createKeycloakUser(EMAIL));

        verify(users, never()).get(anyString());
        verify(users, never()).search(anyString());
        verify(response).close();
    }

    @Test
    void shouldValidateNewIdentityAndPreserveProvisioningFlags() {
        createResponse(201);
        when(response.getLocation()).thenReturn(URI.create("https://keycloak.test/admin/realms/df/users/new-id"));
        when(users.get("new-id")).thenReturn(resource);
        when(resource.toRepresentation()).thenReturn(identity("new-id", EMAIL));

        assertEquals("new-id", service.createKeycloakUser(EMAIL));

        verify(users).create(argThat(user -> EMAIL.equals(user.getEmail()) && EMAIL.equals(user.getUsername())
                && Boolean.valueOf(true).equals(user.isEnabled()) && !user.isEmailVerified()
                && user.getCredentials() == null));
        verify(users, never()).searchByEmail(anyString(), anyBoolean());
        verify(response).close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/not-users/id", "/users/", "/users/id/extra", "/users/%2F", "/users/.",
            "/users/..", "/users/id?query=1", "/users/id#fragment"})
    void shouldRejectMalformedCreationLocation(String location) {
        createResponse(201);
        when(response.getLocation()).thenReturn(location == null ? null : URI.create(location));

        assertThrows(IllegalStateException.class, () -> service.createKeycloakUser(EMAIL));

        verify(users, never()).get(anyString());
        verify(users, never()).searchByEmail(anyString(), anyBoolean());
        verify(response).close();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 204, 400, 403, 500})
    void shouldRejectUnexpectedCreationStatusWithoutFallback(int status) {
        createResponse(status);

        assertThrows(IllegalStateException.class, () -> service.createKeycloakUser(EMAIL));

        verify(users, never()).get(anyString());
        verify(users, never()).searchByEmail(anyString(), anyBoolean());
        verify(response).close();
    }

    static Stream<Arguments> invalidIdentities() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(identity(null, EMAIL)),
                Arguments.of(identity("different-id", EMAIL)),
                Arguments.of(identity("bound-id", null)),
                Arguments.of(identity("bound-id", "someone@example.com")),
                Arguments.of(identity("bound-id", "invitee@example.com.evil")));
    }

    @ParameterizedTest
    @MethodSource("invalidIdentities")
    void shouldRevalidateConflictCandidateById(UserRepresentation identity) {
        createResponse(409);
        when(users.searchByEmail(EMAIL, true)).thenReturn(List.of(identity("bound-id", EMAIL)));
        when(users.get("bound-id")).thenReturn(resource);
        when(resource.toRepresentation()).thenReturn(identity);

        assertThrows(IllegalStateException.class, () -> service.createKeycloakUser(EMAIL));

        verify(resource, never()).update(any());
        verify(response).close();
    }

    @Test
    void shouldRejectDeletedConflictCandidateWithoutRetry() {
        createResponse(409);
        when(users.searchByEmail(EMAIL, true)).thenReturn(List.of(identity("deleted-id", EMAIL)));
        when(users.get("deleted-id")).thenReturn(resource);
        when(resource.toRepresentation()).thenThrow(new NotFoundException());

        assertThrows(IllegalStateException.class, () -> service.createKeycloakUser(EMAIL));

        verify(users).create(any());
        verify(response).close();
    }

    static Stream<RuntimeException> serviceErrors() {
        return Stream.of(new ForbiddenException(), new ServiceUnavailableException(), new ProcessingException("timeout"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectBlankEmailBeforeContactingKeycloak(String email) {
        clearInvocations(realm);
        assertThrows(IllegalArgumentException.class, () -> service.createKeycloakUser(email));
        verifyNoInteractions(realm, users);
    }

    @ParameterizedTest
    @MethodSource("serviceErrors")
    void shouldPropagateCandidateReadFailureWithoutRetry(RuntimeException failure) {
        createResponse(409);
        when(users.searchByEmail(EMAIL, true)).thenReturn(List.of(identity("bound-id", EMAIL)));
        when(users.get("bound-id")).thenReturn(resource);
        when(resource.toRepresentation()).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> service.createKeycloakUser(EMAIL)));

        verify(users).create(any());
        verify(resource, never()).update(any());
        verify(response).close();
    }

    private void createResponse(int status) {
        when(users.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(status);
    }

    private static UserRepresentation identity(String id, String email) {
        var result = new UserRepresentation();
        result.setId(id);
        result.setEmail(email);
        return result;
    }
}
