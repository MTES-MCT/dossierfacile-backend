package fr.dossierfacile.api.dossierfacileapiowner.register;

import fr.dossierfacile.api.dossierfacileapiowner.config.ResourceServerConfig;
import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.user.*;
import fr.dossierfacile.common.entity.Owner;
import jakarta.ws.rs.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OwnerController.class, properties = {
        "callback.http.auth.token=test", "callback.http.auth.token.header.name=X-Api-Key"
})
@ContextConfiguration(classes = {
        OwnerController.class, ResourceServerConfig.class, AuthenticationFacadeImpl.class, KeycloakServiceImpl.class
})
class OwnerAuthenticationRegressionTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private OwnerRepository owners;
    @MockitoBean
    private UserRepository users;
    @MockitoBean
    private OwnerService ownerService;
    @MockitoBean
    private OwnerMapper mapper;
    @MockitoBean
    private OwnerLogService logs;
    @MockitoBean
    private RealmResource realm;
    @MockitoBean
    private JwtDecoder decoder;

    private final UsersResource identityResources = mock(UsersResource.class);
    private final UserResource identityResource = mock(UserResource.class);

    @BeforeEach
    void adminApiIsUnavailable() {
        when(realm.users()).thenReturn(identityResources);
        when(identityResources.get(anyString())).thenReturn(identityResource);
        when(identityResource.toRepresentation()).thenThrow(new ServiceUnavailableException());
    }

    @Test
    void shouldLoadExistingOwnerProfileWhenKeycloakAdminIsUnavailable() throws Exception {
        var owner = Owner.builder().id(1L).email("owner@example.com").keycloakId("owner-id").build();
        when(decoder.decode("valid-token")).thenReturn(jwt("dossier"));
        when(owners.findByKeycloakId("owner-id")).thenReturn(Optional.of(owner));
        when(owners.saveAndFlush(owner)).thenReturn(owner);
        when(mapper.toOwnerModel(owner)).thenReturn(OwnerModel.builder().email(owner.getEmail()).build());

        mvc.perform(get("/api/owner/profile").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(decoder).decode("valid-token");
        verify(ownerService).updateLastLoginDate(owner);
        verify(identityResource, never()).toRepresentation();
        verifyNoInteractions(users);
    }

    @Test
    void shouldPreserveFirstLoginWithoutKeycloakAdminReads() throws Exception {
        when(decoder.decode("valid-token")).thenReturn(jwt("dossier"));
        when(owners.saveAndFlush(any())).thenAnswer(invocation -> {
            Owner owner = invocation.getArgument(0);
            owner.setId(1L);
            return owner;
        });
        when(mapper.toOwnerModel(any())).thenReturn(OwnerModel.builder().email("owner@example.com").build());

        mvc.perform(get("/api/owner/profile").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(owners).saveAndFlush(argThat(owner -> "owner-id".equals(owner.getKeycloakId())
                && "owner@example.com".equals(owner.getEmail())));
        verify(identityResource, never()).toRepresentation();
        verifyNoInteractions(users);
    }

    @Test
    void shouldRejectMissingJwtBeforeReadingOwner() throws Exception {
        mvc.perform(get("/api/owner/profile")).andExpect(status().isUnauthorized());
        verifyNoInteractions(owners, realm, ownerService);
    }

    @Test
    void shouldRejectJwtRejectedByDecoderBeforeReadingOwner() throws Exception {
        when(decoder.decode("invalid-token")).thenThrow(new BadJwtException("invalid or expired token"));
        mvc.perform(get("/api/owner/profile").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(owners, realm, ownerService);
    }

    @Test
    void shouldStillRequireDossierScope() throws Exception {
        when(decoder.decode("wrong-scope")).thenReturn(jwt("other"));
        mvc.perform(get("/api/owner/profile").header("Authorization", "Bearer wrong-scope"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(owners, realm, ownerService);
    }

    private Jwt jwt(String scope) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("owner-id")
                .claim("email", "owner@example.com").claim("scope", scope).build();
    }
}
