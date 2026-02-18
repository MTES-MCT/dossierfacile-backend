package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeycloakCommonServiceImplTest {

    private RealmResource realmResource;
    private UsersResource usersResource;
    private UserResource userResource;
    private KeycloakCommonServiceImpl service;

    @BeforeEach
    void setUp() {
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
    }

    @Test
    void revokeUserConsent_should_call_keycloak_when_consent_exists() {
        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        Tenant tenant = Tenant.builder().id(1L).keycloakId("kc-123").build();
        UserApi userApi = UserApi.builder().id(2L).name("dfconnect-dev").build();

        service.revokeUserConsent(tenant, userApi);

        verify(usersResource).get("kc-123");
        verify(userResource).revokeConsent("dfconnect-dev");
    }

    @Test
    void revokeUserConsent_should_not_throw_when_keycloak_returns_404() {
        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        Tenant tenant = Tenant.builder().id(1L).keycloakId("kc-123").build();
        UserApi userApi = UserApi.builder().id(2L).name("dfconnect-dev").build();

        doThrow(new NotFoundException()).when(userResource).revokeConsent("dfconnect-dev");

        assertThatCode(() -> service.revokeUserConsent(tenant, userApi))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteKeycloakUsers_should_delete_users_with_keycloak_id() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(204);
        when(usersResource.delete("kc-123")).thenReturn(response);

        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        User user1 = Tenant.builder().id(1L).keycloakId("kc-123").build();

        service.deleteKeycloakUsers(List.of(user1));

        verify(usersResource).delete("kc-123");
    }

    @Test
    void deleteKeycloakUsers_should_filter_out_users_with_null_keycloak_id() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(204);
        when(usersResource.delete("kc-123")).thenReturn(response);

        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        User userWithId = Tenant.builder().id(1L).keycloakId("kc-123").build();
        User userWithoutId = Tenant.builder().id(2L).keycloakId(null).build();

        service.deleteKeycloakUsers(List.of(userWithId, userWithoutId));

        verify(usersResource).delete("kc-123");
        verify(usersResource, times(1)).delete(any());
    }

    @Test
    void deleteKeycloakUserById_should_delete_user_by_keycloak_id() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(204);
        when(usersResource.delete("kc-789")).thenReturn(response);

        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        service.deleteKeycloakUserById("kc-789");

        verify(usersResource).delete("kc-789");
    }

    @Test
    void deleteKeycloakUsers_should_handle_non_204_response() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(usersResource.delete("kc-123")).thenReturn(response);

        service = new KeycloakCommonServiceImpl(Optional.of(realmResource));

        User user = Tenant.builder().id(1L).keycloakId("kc-123").build();

        assertThatCode(() -> service.deleteKeycloakUsers(List.of(user)))
                .doesNotThrowAnyException();
    }
}
