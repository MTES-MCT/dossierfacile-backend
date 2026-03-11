package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.exception.controller.CustomRestExceptionHandler;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.FileServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(FileController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestApplication.class,
        ResourceServerConfig.class,
        CustomRestExceptionHandler.class,
        GlobalExceptionHandler.class,
        FileServiceImpl.class
})
@TestPropertySource(properties = {
        "dossierfacile.common.global.exception.handler=true",
        "resource.server.config.csp=default-src 'self'"
})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileRepository fileRepository;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private Producer producer;

    @MockitoBean
    private DocumentIAService documentIAService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static FileControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    private static final Long FILE_ID = 1L;

    private static FileControllerTestFixture buildTestFixture() {
        ApartmentSharing sharingAlone = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
        Tenant tenantAlone = Tenant.builder().id(1L).apartmentSharing(sharingAlone).build();

        ApartmentSharing sharingGroup = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.GROUP).build();
        Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharingGroup).build();
        Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharingGroup).build();
        sharingGroup.setTenants(List.of(tenant1, tenant2));

        ApartmentSharing sharingCouple = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.COUPLE).build();
        Tenant tenantCouple1 = Tenant.builder().id(1L).apartmentSharing(sharingCouple).build();
        Tenant tenantCouple2 = Tenant.builder().id(2L).apartmentSharing(sharingCouple).build();
        sharingCouple.setTenants(List.of(tenantCouple1, tenantCouple2));

        StorageFile storageFile = new StorageFile();
        storageFile.setName("file.pdf");
        storageFile.setContentType("application/pdf");

        Document document = Document.builder().id(1L).tenant(tenant1).build();
        File file = File.builder().id(FILE_ID).storageFile(storageFile).document(document).build();
        document.getFiles().add(file);

        Document documentCoTenantGroup = Document.builder().id(2L).tenant(tenant2).build();
        File fileCoTenantGroup = File.builder().id(FILE_ID).storageFile(storageFile).document(documentCoTenantGroup).build();
        documentCoTenantGroup.getFiles().add(fileCoTenantGroup);

        Document documentCoTenantCouple = Document.builder().id(2L).tenant(tenantCouple2).build();
        File fileCoTenantCouple = File.builder().id(FILE_ID).storageFile(storageFile).document(documentCoTenantCouple).build();
        documentCoTenantCouple.getFiles().add(fileCoTenantCouple);

        Document documentAlone = Document.builder().id(1L).tenant(tenantAlone).build();
        File fileAlone = File.builder().id(FILE_ID).storageFile(storageFile).document(documentAlone).build();
        documentAlone.getFiles().add(fileAlone);

        return new FileControllerTestFixture(
                tenantAlone, fileAlone, sharingGroup, tenant1, tenant2, sharingCouple, tenantCouple1, tenantCouple2,
                storageFile, document, file, documentCoTenantGroup, fileCoTenantGroup, documentCoTenantCouple, fileCoTenantCouple
        );
    }

    private record FileControllerTestFixture(
            Tenant tenantAlone,
            File fileAlone,
            ApartmentSharing sharingGroup,
            Tenant tenant1,
            Tenant tenant2,
            ApartmentSharing sharingCouple,
            Tenant tenantCouple1,
            Tenant tenantCouple2,
            StorageFile storageFile,
            Document document,
            File file,
            Document documentCoTenantGroup,
            File fileCoTenantGroup,
            Document documentCoTenantCouple,
            File fileCoTenantCouple
    ) {
        File fileForCouple() { return fileCoTenantCouple; }
    }

    @Nested
    class GetFileResourceTest {

        record GetFileResourceParam(ApplicationType applicationType, boolean isOwnFile, boolean fileFound) {}

        static List<Arguments> provideGetFileResourceParameters() {
            var jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));
            var f = buildTestFixture();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 200 when GROUP tenant owns the file",
                            new ControllerParameter<>(
                                    new GetFileResourceParam(ApplicationType.GROUP, true, true),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenant1());
                                        when(self.fileRepository.findByIdForTenant(FILE_ID, f.tenant1().getId())).thenReturn(Optional.of(f.file()));
                                        try {
                                            when(self.fileStorageService.download(f.storageFile()))
                                                    .thenReturn(new ByteArrayInputStream("content".getBytes()));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when GROUP tenant requests co-tenant file",
                            new ControllerParameter<>(
                                    new GetFileResourceParam(ApplicationType.GROUP, false, false),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenant1());
                                        when(self.fileRepository.findByIdForTenant(FILE_ID, f.tenant1().getId())).thenReturn(Optional.empty());
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when COUPLE tenant requests co-tenant file",
                            new ControllerParameter<>(
                                    new GetFileResourceParam(ApplicationType.COUPLE, false, true),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenantCouple1());
                                        when(self.fileRepository.findByIdForAppartmentSharing(FILE_ID, f.tenantCouple1().getApartmentSharing().getId()))
                                                .thenReturn(Optional.of(f.fileForCouple()));
                                        try {
                                            when(self.fileStorageService.download(f.fileForCouple().getStorageFile()))
                                                    .thenReturn(new ByteArrayInputStream("content".getBytes()));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 401 when no JWT is provided",
                            new ControllerParameter<>(
                                    new GetFileResourceParam(ApplicationType.GROUP, true, true),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetFileResourceParameters")
        void parameterizedTests(ControllerParameter<GetFileResourceParam> parameter) throws Exception {
            if (parameter.getRequestPostProcessor() == null) {
                mockMvc.perform(get("/api/file/resource/{id}", FILE_ID))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(parameter.getStatus()));
            } else {
                ParameterizedTestHelper.runControllerTest(
                        mockMvc,
                        get("/api/file/resource/{id}", FILE_ID).with(parameter.getRequestPostProcessor()),
                        parameter
                );
            }
        }
    }

    @Nested
    class DeleteFileTest {

        record DeleteFileParam() {}

        static List<Arguments> provideDeleteFileParameters() {
            var jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));
            var f = buildTestFixture();

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 200 when tenant deletes own file",
                            new ControllerParameter<>(
                                    new DeleteFileParam(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenantAlone());
                                        when(self.fileRepository.findByIdForTenant(FILE_ID, f.tenantAlone().getId())).thenReturn(Optional.of(f.fileAlone()));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 404 when group tenant tries to delete coTenant file",
                            new ControllerParameter<>(
                                    new DeleteFileParam(),
                                    404,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenant1());
                                        when(self.fileRepository.findByIdForTenant(FILE_ID, f.tenant1().getId())).thenReturn(Optional.empty());
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when group tenant deletes own file",
                            new ControllerParameter<>(
                                    new DeleteFileParam(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenant1());
                                        when(self.fileRepository.findByIdForTenant(FILE_ID, f.tenant1().getId())).thenReturn(Optional.of(f.file()));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when couple tenant deletes coTenant file",
                            new ControllerParameter<>(
                                    new DeleteFileParam(),
                                    200,
                                    jwtTokenWithDossier,
                                    (v) -> {
                                        when(self.authenticationFacade.getLoggedTenant()).thenReturn(f.tenantCouple1());
                                        when(self.fileRepository.findByIdForAppartmentSharing(FILE_ID, f.tenantCouple1().getApartmentSharing().getId()))
                                                .thenReturn(Optional.of(f.fileForCouple()));
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 401 when no JWT is provided",
                            new ControllerParameter<>(
                                    new DeleteFileParam(),
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideDeleteFileParameters")
        void parameterizedTests(ControllerParameter<DeleteFileParam> parameter) throws Exception {
            if (parameter.getRequestPostProcessor() == null) {
                mockMvc.perform(delete("/api/file/{id}", FILE_ID))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(parameter.getStatus()));
            } else {
                ParameterizedTestHelper.runControllerTest(
                        mockMvc,
                        delete("/api/file/{id}", FILE_ID).with(parameter.getRequestPostProcessor()),
                        parameter
                );
            }
        }
    }
}
