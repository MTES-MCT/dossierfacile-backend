package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.user.UserRepository;
import fr.dossierfacile.common.entity.Owner;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("KeycloakServiceImpl Tests")
class KeycloakServiceImplTest {

    private RealmResource realmResource;
    private UsersResource usersResource;
    private UserResource userResource;
    private UserRepository userRepository;
    private KeycloakServiceImpl keycloakService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String EXISTING_KEYCLOAK_ID = "existing-keycloak-id";
    private static final String NEW_KEYCLOAK_ID = "new-keycloak-id";

    @BeforeEach
    void setUp() {
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        userRepository = mock(UserRepository.class);

        when(realmResource.users()).thenReturn(usersResource);
        keycloakService = new KeycloakServiceImpl(realmResource, userRepository);
    }

    @Test
    @DisplayName("Scenario 1: User (Owner) has keycloakId null, but Keycloak user with email exists - should link to existing user")
    void test_confirmKeycloakUser_keycloakIdNull_userExistsInKeycloak_shouldLinkToExisting() {
        // Arrange
        Owner user = Owner.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .keycloakId(null)
                .build();

        // Mock: When keycloakId is null, get(null) will throw NullPointerException, so we create a new user
        // The implementation doesn't search by email when keycloakId is null - it just creates a new user
        when(usersResource.get(null)).thenThrow(new NullPointerException("keycloakId is null"));
        
        UserRepresentation newUserRep = new UserRepresentation();
        newUserRep.setId(NEW_KEYCLOAK_ID);
        newUserRep.setEmail(TEST_EMAIL);
        newUserRep.setEnabled(false);
        newUserRep.setEmailVerified(false);

        UserResource newUserResource = mock(UserResource.class);
        when(usersResource.get(NEW_KEYCLOAK_ID)).thenReturn(newUserResource);
        when(newUserResource.toRepresentation()).thenReturn(newUserRep);

        // Mock create user response
        Response response = mock(Response.class);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.CONFLICT.value());
        when(usersResource.search(TEST_EMAIL)).thenReturn(Collections.singletonList(newUserRep));
        doNothing().when(response).close();

        when(userRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        keycloakService.confirmKeycloakUser(user);

        // Assert
        assertEquals(NEW_KEYCLOAK_ID, user.getKeycloakId(), "User keycloakId should be set to new Keycloak user ID");
        verify(userRepository, times(1)).save(any(Owner.class));
        verify(usersResource, times(1)).get(null); // This will throw NullPointerException
        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(usersResource, times(2)).get(NEW_KEYCLOAK_ID); // Called to get representation after creation and to update
        verify(newUserResource, times(1)).toRepresentation();
        verify(newUserResource, times(1)).update(any(UserRepresentation.class));
        
        // Verify user is enabled and email verified
        verify(newUserResource).update(argThat(rep -> 
            rep.isEnabled() && rep.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Scenario 2: User has keycloakId not null, but Keycloak user with this id does not exist - should create user")
    void test_confirmKeycloakUser_keycloakIdNotNull_userDoesNotExist_shouldCreateUser() {
        // Arrange
        Owner user = Owner.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .keycloakId(EXISTING_KEYCLOAK_ID)
                .build();

        // Mock: user does not exist in Keycloak with the keycloakId
        when(usersResource.get(EXISTING_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Mock: search returns empty (no user with email)
        when(usersResource.search(TEST_EMAIL)).thenReturn(Collections.emptyList());

        // Mock: create new user
        UserRepresentation newUserRep = new UserRepresentation();
        newUserRep.setId(NEW_KEYCLOAK_ID);
        newUserRep.setEmail(TEST_EMAIL);
        newUserRep.setEnabled(false);
        newUserRep.setEmailVerified(false);

        UserResource newUserResource = mock(UserResource.class);
        when(usersResource.get(NEW_KEYCLOAK_ID)).thenReturn(newUserResource);
        when(newUserResource.toRepresentation()).thenReturn(newUserRep);

        // Mock create user response
        Response response = mock(Response.class);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn("http://localhost:8085/auth/admin/realms/test/users/" + NEW_KEYCLOAK_ID);
        doNothing().when(response).close();

        when(userRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        keycloakService.confirmKeycloakUser(user);

        // Assert
        assertEquals(NEW_KEYCLOAK_ID, user.getKeycloakId(), "User keycloakId should be set to new Keycloak user ID");
        verify(userRepository, times(1)).save(user);
        verify(usersResource, times(1)).get(EXISTING_KEYCLOAK_ID);
        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(usersResource, times(2)).get(NEW_KEYCLOAK_ID); // Called to get representation after creation and to update
        verify(newUserResource, times(1)).toRepresentation();
        verify(newUserResource, times(1)).update(any(UserRepresentation.class));
        
        // Verify user is enabled and email verified
        verify(newUserResource).update(argThat(rep -> 
            rep.isEnabled() && rep.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Scenario 3: User has keycloakId null and Keycloak user with email does not exist - should create new user and update entity")
    void test_confirmKeycloakUser_keycloakIdNull_userDoesNotExist_shouldCreateNewUserAndUpdateEntity() {
        // Arrange
        Owner user = Owner.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .keycloakId(null)
                .build();

        // Mock: When keycloakId is null, get(null) will throw NullPointerException, so we create a new user
        // The implementation doesn't search by email when keycloakId is null - it just creates a new user
        when(usersResource.get(null)).thenThrow(new NullPointerException("keycloakId is null"));
        
        UserRepresentation newUserRep = new UserRepresentation();
        newUserRep.setId(NEW_KEYCLOAK_ID);
        newUserRep.setEmail(TEST_EMAIL);
        newUserRep.setEnabled(false);
        newUserRep.setEmailVerified(false);

        UserResource newUserResource = mock(UserResource.class);
        when(usersResource.get(NEW_KEYCLOAK_ID)).thenReturn(newUserResource);
        when(newUserResource.toRepresentation()).thenReturn(newUserRep);

        // Mock create user response
        Response response = mock(Response.class);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn("http://localhost:8085/auth/admin/realms/test/users/" + NEW_KEYCLOAK_ID);
        doNothing().when(response).close();

        when(userRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        keycloakService.confirmKeycloakUser(user);

        // Assert
        assertNotNull(user.getKeycloakId(), "User keycloakId should not be null");
        assertEquals(NEW_KEYCLOAK_ID, user.getKeycloakId(), "User keycloakId should be set to new Keycloak user ID");
        verify(userRepository, times(1)).save(user);
        verify(usersResource, times(1)).get(null); // This will throw NullPointerException
        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(usersResource, times(2)).get(NEW_KEYCLOAK_ID); // Called to get representation after creation and to update
        verify(newUserResource, times(1)).toRepresentation();
        verify(newUserResource, times(1)).update(any(UserRepresentation.class));
        
        // Verify user is enabled and email verified
        verify(newUserResource).update(argThat(rep -> 
            rep.isEnabled() && rep.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("User has valid keycloakId and user exists in Keycloak - should just enable and verify")
    void test_confirmKeycloakUser_keycloakIdValid_userExists_shouldJustEnableAndVerify() {
        // Arrange
        Owner user = Owner.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .keycloakId(EXISTING_KEYCLOAK_ID)
                .build();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(EXISTING_KEYCLOAK_ID);
        userRep.setEmail(TEST_EMAIL);
        userRep.setEnabled(false);
        userRep.setEmailVerified(false);

        when(usersResource.get(EXISTING_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);

        // Act
        keycloakService.confirmKeycloakUser(user);

        // Assert
        assertEquals(EXISTING_KEYCLOAK_ID, user.getKeycloakId(), "User keycloakId should remain unchanged");
        verify(userRepository, never()).save(any(Owner.class));
        verify(usersResource, never()).search(anyString());
        verify(usersResource, never()).create(any(UserRepresentation.class));
        verify(usersResource, times(2)).get(EXISTING_KEYCLOAK_ID); // Called once to get representation, once to update
        verify(userResource, times(1)).toRepresentation();
        verify(userResource, times(1)).update(any(UserRepresentation.class));
        
        // Verify user is enabled and email verified
        verify(userResource).update(argThat(rep -> 
            rep.isEnabled() && rep.isEmailVerified()
        ));
    }
}
