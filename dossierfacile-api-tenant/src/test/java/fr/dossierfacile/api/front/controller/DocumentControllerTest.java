package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.MethodSecurityConfig;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.google.gson.Gson;
import fr.dossierfacile.api.front.form.CommentAnalysisForm;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(DocumentController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        ResourceServerConfig.class,
        MethodSecurityConfig.class,
        CustomRestExceptionHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "dossierfacile.common.global.exception.handler=true",
        "resource.server.config.csp=default-src 'self'"
})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private DocumentRepository documentRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private TenantMapper tenantMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static DocumentControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    private static final String DOCUMENT_NAME = "abc-123.pdf";

    @Nested
    class GetDocumentResourceTests {

        record GetDocumentResourceTestParameter() {
        }

        static List<Arguments> provideGetDocumentResourceParameters() {
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier =
                    jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .apartmentSharing(apartmentSharing)
                    .build();

            StorageFile watermarkFile = new StorageFile();
            watermarkFile.setName("watermark.pdf");

            Document document = Document.builder()
                    .id(1L)
                    .name(DOCUMENT_NAME)
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                    .tenant(tenant)
                    .build();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 200 when authenticated tenant owns the document",
                            new ControllerParameter<>(
                                    new GetDocumentResourceTestParameter(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(self.documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant))
                                                .thenReturn(document);
                                        try {
                                            when(self.fileStorageService.download(watermarkFile))
                                                    .thenReturn(new ByteArrayInputStream("PDF content".getBytes()));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when authenticated tenant does not own the document",
                            new ControllerParameter<>(
                                    new GetDocumentResourceTestParameter(),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(self.documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant))
                                                .thenThrow(new AccessDeniedException("Not authorized"));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 401 when no JWT is provided",
                            new ControllerParameter<>(
                                    new GetDocumentResourceTestParameter(),
                                    401,
                                    null, // no JWT
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when document does not exist",
                            new ControllerParameter<>(
                                    new GetDocumentResourceTestParameter(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(tenant);
                                        when(self.documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant))
                                                .thenThrow(new DocumentNotFoundException(DOCUMENT_NAME));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetDocumentResourceParameters")
        void parameterizedTests(ControllerParameter<GetDocumentResourceTestParameter> parameter) throws Exception {
            var mockMvcRequestBuilder = get("/api/document/resource/{documentName}", DOCUMENT_NAME)
                    .contentType("application/pdf");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class CommentAnalysisTest {

        record CommentAnalysisParam(CommentAnalysisForm form) {}

        static List<Arguments> provideCommentAnalysisParameters() {
            var jwtTokenWithDossier = jwt().jwt(jwt -> jwt.subject("keycloak-user-id"))
                    .authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            CommentAnalysisForm formWithUnauthorizedTenant = new CommentAnalysisForm(1L, 99L, "comment");
            CommentAnalysisForm formWithTooLongComment = new CommentAnalysisForm(1L, 1L, "a".repeat(2001));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 403 when tenant has no permission on requested tenantId",
                            new ControllerParameter<>(
                                    new CommentAnalysisParam(formWithUnauthorizedTenant),
                                    403,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess("keycloak-user-id", 99L)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 400 when comment exceeds 2000 characters",
                            new ControllerParameter<>(
                                    new CommentAnalysisParam(formWithTooLongComment),
                                    400,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.tenantPermissionsService.canAccess("keycloak-user-id", 1L)).thenReturn(true);
                                        return v;
                                    },
                                    List.of(
                                            jsonPath("$.errors").isArray(),
                                            jsonPath("$.errors[0]").value("comment: the number of chars must be less than or equal to 2000")
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideCommentAnalysisParameters")
        void parameterizedTests(ControllerParameter<CommentAnalysisParam> parameter) throws Exception {
            var mockMvcRequestBuilder = post("/api/document/commentAnalysis")
                    .contentType("application/json")
                    .content(new Gson().toJson(parameter.getParameterData().form()));

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}
