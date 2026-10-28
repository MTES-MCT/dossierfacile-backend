package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServiceUnavailableException;
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

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakPasswordBindingTest {
    private static final String BOUND_ID = "bound-keycloak-id";
    private static final String EMAIL = "invitee@example.com";
    private static final String PASSWORD = "test-password";

    private RealmResource realmResource;
    private UsersResource usersResource;
    private UserResource userResource;
    private KeycloakServiceImpl keycloakService;

    @BeforeEach
    void setUp() {
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(BOUND_ID)).thenReturn(userResource);
        keycloakService = new KeycloakServiceImpl(realmResource, mock(UserRepository.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {EMAIL, "INVITEE@EXAMPLE.COM"})
    void shouldUpdateOnlyTheBoundIdentity(String authoritativeEmail) {
        var identity = identity(BOUND_ID, authoritativeEmail);
        identity.setRequiredActions(List.of("UPDATE_PROFILE"));
        when(userResource.toRepresentation()).thenReturn(identity);

        assertTrue(keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD));

        verify(userResource).update(same(identity));
        assertEquals(BOUND_ID, identity.getId());
        assertEquals(authoritativeEmail, identity.getEmail());
        assertEquals(List.of("UPDATE_PROFILE"), identity.getRequiredActions());
        assertTrue(identity.isEnabled());
        assertTrue(identity.isEmailVerified());
        assertEquals(1, identity.getCredentials().size());
        assertEquals("password", identity.getCredentials().getFirst().getType());
        assertEquals(PASSWORD, identity.getCredentials().getFirst().getValue());
        assertFalse(identity.getCredentials().getFirst().isTemporary());
        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingIdBeforeContactingKeycloak(String id) {
        assertFalse(keycloakService.createPasswordForBoundUser(id, EMAIL, PASSWORD));
        verifyNoInteractions(realmResource, usersResource, userResource);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingExpectedEmailBeforeContactingKeycloak(String email) {
        assertFalse(keycloakService.createPasswordForBoundUser(BOUND_ID, email, PASSWORD));
        verifyNoInteractions(realmResource, usersResource, userResource);
    }

    static Stream<Arguments> mismatchedIdentities() {
        return Stream.of(
                Arguments.of("another-keycloak-id", EMAIL),
                Arguments.of("BOUND-KEYCLOAK-ID", EMAIL),
                Arguments.of(null, EMAIL),
                Arguments.of(BOUND_ID, "xinvitee@example.com"),
                Arguments.of(BOUND_ID, "invitee+alias@example.com"),
                Arguments.of(BOUND_ID, "invitéé@example.com"),
                Arguments.of(BOUND_ID, " invitee@example.com"),
                Arguments.of(BOUND_ID, null),
                Arguments.of(BOUND_ID, "")
        );
    }

    @ParameterizedTest
    @MethodSource("mismatchedIdentities")
    void shouldRejectMismatchedIdentityWithoutMutation(String id, String email) {
        var identity = identity(id, email);
        when(userResource.toRepresentation()).thenReturn(identity);

        assertFalse(keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD));

        assertFalse(identity.isEnabled());
        assertFalse(identity.isEmailVerified());
        assertNull(identity.getCredentials());
        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    @Test
    void shouldRejectDeletedIdentityWithoutSearchingOrCreating() {
        when(userResource.toRepresentation()).thenThrow(new NotFoundException());

        assertFalse(keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD));

        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    @Test
    void shouldRejectMissingRepresentationWithoutMutation() {
        when(userResource.toRepresentation()).thenReturn(null);

        assertFalse(keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD));

        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    static Stream<RuntimeException> lookupFailures() {
        return Stream.of(new ForbiddenException(), new ServiceUnavailableException(), new ProcessingException("timeout"));
    }

    @ParameterizedTest
    @MethodSource("lookupFailures")
    void shouldPropagateLookupFailuresWithoutFallback(RuntimeException failure) {
        when(userResource.toRepresentation()).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD)));

        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    static Stream<RuntimeException> updateFailures() {
        return Stream.of(new NotFoundException(), new ServiceUnavailableException(), new ProcessingException("timeout"));
    }

    @ParameterizedTest
    @MethodSource("updateFailures")
    void shouldPropagateUpdateFailuresWithoutRetry(RuntimeException failure) {
        var identity = identity(BOUND_ID, EMAIL);
        when(userResource.toRepresentation()).thenReturn(identity);
        doThrow(failure).when(userResource).update(any(UserRepresentation.class));

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> keycloakService.createPasswordForBoundUser(BOUND_ID, EMAIL, PASSWORD)));

        verify(userResource).update(same(identity));
        verifySingleLookup();
        verifyNoMoreInteractions(userResource);
    }

    private void verifySingleLookup() {
        verify(usersResource).get(BOUND_ID);
        verifyNoMoreInteractions(usersResource);
        verify(userResource).toRepresentation();
    }

    private UserRepresentation identity(String id, String email) {
        var identity = new UserRepresentation();
        identity.setId(id);
        identity.setEmail(email);
        identity.setEnabled(false);
        identity.setEmailVerified(false);
        return identity;
    }
}
