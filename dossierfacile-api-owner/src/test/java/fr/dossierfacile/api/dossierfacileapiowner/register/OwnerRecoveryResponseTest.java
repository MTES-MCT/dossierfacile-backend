package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.user.*;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnerRecoveryResponseTest {
    private final OwnerRepository owners = mock(OwnerRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RealmResource realm = mock(RealmResource.class);
    private final UsersResource identities = mock(UsersResource.class);
    private final UserResource resource = mock(UserResource.class);
    private final PasswordRecoveryTokenService tokens = mock(PasswordRecoveryTokenService.class);
    private final MailService mail = mock(MailService.class);
    private final Owner owner = Owner.builder().id(1L).email("owner@example.com").keycloakId("owner-id").build();
    private final RegisterServiceImpl service = new RegisterServiceImpl(owners, mock(BCryptPasswordEncoder.class),
            new KeycloakServiceImpl(realm, users), mock(OwnerMapper.class), mail, mock(ConfirmationTokenRepository.class),
            mock(ConfirmationTokenService.class), tokens, users, mock(UserRoleService.class),
            mock(PasswordRecoveryTokenRepository.class), mock(OwnerLogService.class));
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(service)).build();
        when(realm.users()).thenReturn(identities);
        when(identities.get("owner-id")).thenReturn(resource);
        when(owners.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(tokens.create(owner)).thenReturn(PasswordRecoveryToken.builder().user(owner).token("recovery-token").build());
    }

    @Test
    void shouldPreserveGenericResponseForUnknownOwner() throws Exception {
        mvc.perform(post("/api/register/forgotPassword").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}")).andExpect(status().isOk());
        verifyNoInteractions(realm, tokens, mail);
    }

    @Test
    void shouldPreserveRecoveryWithoutNewAdminReadsForAnExistingBinding() throws Exception {
        when(resource.toRepresentation()).thenThrow(new ServiceUnavailableException());

        mvc.perform(post("/api/register/forgotPassword").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"owner@example.com\"}")).andExpect(status().isOk());

        var order = inOrder(owners, tokens, mail);
        order.verify(owners).save(owner);
        order.verify(tokens).create(owner);
        order.verify(mail).sendEmailNewPassword(eq(owner), any());
        verify(resource, never()).toRepresentation();
        assertEquals("owner-id", owner.getKeycloakId());
    }

    @Test
    void shouldPreserveRecoveryForAnOwnerWithoutKeycloakId() throws Exception {
        owner.setKeycloakId(null);
        var response = mock(Response.class);
        when(identities.create(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("/admin/realms/owner/users/owner-id"));
        var identity = new UserRepresentation();
        identity.setId("owner-id");
        identity.setEmail(owner.getEmail());
        when(resource.toRepresentation()).thenReturn(identity);

        mvc.perform(post("/api/register/forgotPassword").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"owner@example.com\"}")).andExpect(status().isOk());

        assertEquals("owner-id", owner.getKeycloakId());
        var order = inOrder(owners, tokens, mail);
        order.verify(owners).save(owner);
        order.verify(tokens).create(owner);
        order.verify(mail).sendEmailNewPassword(eq(owner), any());
        verify(response).close();
    }
}
