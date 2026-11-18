package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.common.entity.Owner;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakServiceImplTest {
    private static final String EMAIL = "owner@example.com";
    private final RealmResource realm = mock(RealmResource.class);
    private final UsersResource users = mock(UsersResource.class);
    private final UserResource existingResource = mock(UserResource.class);
    private final UserResource newResource = mock(UserResource.class);
    private final UserRepository repository = mock(UserRepository.class);
    private final KeycloakServiceImpl service = new KeycloakServiceImpl(realm, repository);

    @BeforeEach
    void setUp() {
        when(realm.users()).thenReturn(users);
        when(users.get("existing-id")).thenReturn(existingResource);
        when(users.get("new-id")).thenReturn(newResource);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"existing-id"})
    void shouldPreserveConfirmationForMissingOrDeletedBinding(String oldId) {
        var owner = owner(oldId);
        when(existingResource.toRepresentation()).thenThrow(new NotFoundException());
        var identity = identity("new-id");
        when(newResource.toRepresentation()).thenReturn(identity);
        var response = mock(Response.class);
        when(users.create(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(409);
        when(users.searchByEmail(EMAIL, true)).thenReturn(List.of(identity));

        service.confirmKeycloakUser(owner);

        assertEquals("new-id", owner.getKeycloakId());
        verify(repository).save(owner);
        verify(newResource).update(argThat(rep -> rep.isEnabled() && rep.isEmailVerified()));
        verify(existingResource, never()).update(any());
        verify(response).close();
    }

    @Test
    void shouldPreserveConfirmationForExistingIdentity() {
        var owner = owner("existing-id");
        when(existingResource.toRepresentation()).thenReturn(identity("existing-id"));

        service.confirmKeycloakUser(owner);

        assertEquals("existing-id", owner.getKeycloakId());
        verify(existingResource).update(argThat(rep -> rep.isEnabled() && rep.isEmailVerified()));
        verifyNoInteractions(repository);
        verify(users, never()).create(any());
    }

    @Test
    void shouldPreserveNewOwnerRegistration() {
        newIdentityResponse();

        assertEquals("new-id", service.createKeycloakUserAccountCreation(form(), owner(null)));

        verify(users).create(argThat(rep -> EMAIL.equals(rep.getEmail()) && !rep.isEnabled() && !rep.isEmailVerified()
                && rep.getCredentials().size() == 1 && "password".equals(rep.getCredentials().getFirst().getValue())));
        verify(users, never()).delete(anyString());
    }

    @Test
    void shouldPreservePendingOwnerReregistration() {
        newIdentityResponse();

        assertEquals("new-id", service.createKeycloakUserAccountCreation(form(), owner("existing-id")));

        var order = inOrder(users);
        order.verify(users).delete("existing-id");
        order.verify(users).create(any());
    }

    private void newIdentityResponse() {
        var response = mock(Response.class);
        when(users.create(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("/admin/realms/df/users/new-id"));
        when(newResource.toRepresentation()).thenReturn(identity("new-id"));
    }

    private Owner owner(String id) {
        return Owner.builder().id(1L).email(EMAIL).keycloakId(id).build();
    }

    private AccountForm form() {
        var form = new AccountForm();
        form.setEmail(EMAIL);
        form.setPassword("password");
        return form;
    }

    private UserRepresentation identity(String id) {
        var identity = new UserRepresentation();
        identity.setId(id);
        identity.setEmail(EMAIL);
        identity.setEnabled(false);
        identity.setEmailVerified(false);
        return identity;
    }
}
