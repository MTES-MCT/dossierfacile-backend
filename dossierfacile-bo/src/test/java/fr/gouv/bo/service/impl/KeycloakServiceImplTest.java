package fr.gouv.bo.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceImplTest {

    private static final String EMAIL = "namespace1.e2e-alone@inbox.testmail.app";
    private static final String KEYCLOAK_ID = "kc-user-id";

    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;

    private KeycloakServiceImpl keycloakService;

    private final UserRepresentation user = new UserRepresentation();

    @BeforeEach
    void setUp() {
        keycloakService = new KeycloakServiceImpl(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        user.setId(KEYCLOAK_ID);
        user.setEmailVerified(false);
    }

    @Test
    void findUserByEmail_searchesForAnExactMatch() {
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(user));

        assertThat(keycloakService.findUserByEmail(EMAIL)).contains(user);
    }

    @Test
    void markEmailAsVerified_setsFlagAndRemovesRequiredAction() {
        user.setRequiredActions(new ArrayList<>(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD")));
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(user));
        when(usersResource.get(KEYCLOAK_ID)).thenReturn(userResource);

        boolean verified = keycloakService.markEmailAsVerified(EMAIL);

        assertThat(verified).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRequiredActions()).containsExactly("UPDATE_PASSWORD");
        verify(userResource).update(user);
    }

    @Test
    void markEmailAsVerified_returnsFalseWhenUserDoesNotExist() {
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(Collections.emptyList());

        assertThat(keycloakService.markEmailAsVerified(EMAIL)).isFalse();
        verify(usersResource, never()).get(anyString());
    }

    @Test
    void deleteKeycloakUserByEmail_deletesTheUserWhenFound() {
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(user));

        assertThat(keycloakService.deleteKeycloakUserByEmail(EMAIL)).isTrue();
        verify(usersResource).delete(KEYCLOAK_ID);
    }

    @Test
    void deleteKeycloakUserByEmail_returnsFalseWhenUserDoesNotExist() {
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(Collections.emptyList());

        assertThat(keycloakService.deleteKeycloakUserByEmail(EMAIL)).isFalse();
        verify(usersResource, never()).delete(anyString());
    }
}
