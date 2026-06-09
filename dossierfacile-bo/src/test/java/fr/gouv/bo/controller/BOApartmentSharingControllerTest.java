package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.gouv.bo.dto.ApartmentSharingLinkEnrichedDTO;
import fr.gouv.bo.dto.LinkLogDTO;
import fr.gouv.bo.security.BOAccessDenied;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOApartmentSharingControllerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private ApartmentSharingLinkService apartmentSharingLinkService;

    @Mock
    private UserApiService userApiService;

    @Mock
    private DocumentService documentService;

    @Mock
    private TenantLogService logService;

    @Mock
    private LinkLogRepository linkLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private BOApplicationAccessService applicationAccessService;

    @Mock
    private ApartmentSharingCommonService applicationSharingService;

    private BOApartmentSharingController controller;

    @BeforeEach
    void setUp() {
        controller = new BOApartmentSharingController(
                tenantService,
                apartmentSharingLinkService,
                userApiService,
                documentService,
                logService,
                linkLogRepository,
                userService,
                applicationAccessService,
                applicationSharingService
        );
        ReflectionTestUtils.setField(controller, "tenantBaseUrl", "https://example.com");
    }


    @Test
    void enrichApartmentSharingLinks_shouldCountDownloadsBasedOnDocumentLogsOnly() throws Exception {
        // Given
        ApartmentSharingLink link = createLink(1L, ApartmentSharingLinkType.LINK, false);

        LinkLog documentLog1 = createLinkLog(link.getToken(), LinkType.DOCUMENT);
        LinkLog documentLog2 = createLinkLog(link.getToken(), LinkType.DOCUMENT);
        LinkLog documentLog3 = createLinkLog(link.getToken(), LinkType.DOCUMENT);
        LinkLog fullAppLog = createLinkLog(link.getToken(), LinkType.FULL_APPLICATION);
        LinkLog lightAppLog = createLinkLog(link.getToken(), LinkType.LIGHT_APPLICATION);
        LinkLog enabledLinkLog = createLinkLog(link.getToken(), LinkType.ENABLED_LINK);

        when(linkLogRepository.findByApartmentSharing(any(ApartmentSharing.class)))
                .thenReturn(List.of(documentLog1, documentLog2, documentLog3, fullAppLog, lightAppLog, enabledLinkLog));

        // When
        List<ApartmentSharingLinkEnrichedDTO> result = invokeEnrichMethod(List.of(link));

        // Then
        assertThat(result).hasSize(1);
        ApartmentSharingLinkEnrichedDTO enrichedLink = result.getFirst();

        // Verify that nbDownloads counts ONLY DOCUMENT type logs
        assertThat(enrichedLink.getNbDownloads()).isEqualTo(3L);

        // Verify that nbVisits counts all visit types (DOCUMENT, FULL_APPLICATION, LIGHT_APPLICATION)
        assertThat(enrichedLink.getNbVisits()).isEqualTo(5L);

        // Verify that accessLogs contains all visit types
        assertThat(enrichedLink.getAccessLogs()).hasSize(5)
                .extracting(LinkLogDTO::getLinkType)
                .containsExactlyInAnyOrder(
                    LinkType.DOCUMENT,
                    LinkType.DOCUMENT,
                    LinkType.DOCUMENT,
                    LinkType.FULL_APPLICATION,
                    LinkType.LIGHT_APPLICATION
                );
    }

    private ApartmentSharingLink createLink(Long id, ApartmentSharingLinkType type, boolean deleted) {
        ApartmentSharingLink link = new ApartmentSharingLink();
        link.setId(id);
        link.setLinkType(type);
        link.setDeleted(deleted);
        link.setToken(UUID.randomUUID());
        link.setFullData(true);
        link.setApartmentSharing(new ApartmentSharing());
        return link;
    }

    private LinkLog createLinkLog(UUID token, LinkType linkType) {
        LinkLog log = new LinkLog();
        log.setToken(token);
        log.setLinkType(linkType);
        log.setCreationDate(LocalDateTime.now());
        return log;
    }

    @SuppressWarnings("unchecked")
    private List<ApartmentSharingLinkEnrichedDTO> invokeEnrichMethod(List<ApartmentSharingLink> links) throws Exception {
        Method method = BOApartmentSharingController.class.getDeclaredMethod("enrichApartmentSharingLinks", List.class);
        method.setAccessible(true);
        return (List<ApartmentSharingLinkEnrichedDTO>) method.invoke(controller, links);
    }

    // -------------------------------------------------------------------------
    // view() — access control
    // -------------------------------------------------------------------------

    @Nested
    class ViewEndpoint {

        private static final Long APARTMENT_SHARING_ID = 99L;

        @Test
        void view_whenAccessServiceThrowsAccessDenied_propagatesException() {
            UserPrincipal principal = operatorPrincipal();
            ApartmentSharing apartmentSharing = apartmentSharing(APARTMENT_SHARING_ID);
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkAndLogApartmentSharingAccess(any(), eq(APARTMENT_SHARING_ID));

            assertThatThrownBy(() -> controller.view(new ExtendedModelMap(), APARTMENT_SHARING_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }

        @Test
        void view_whenApartmentSharingDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = supportPrincipal();
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.view(new ExtendedModelMap(), APARTMENT_SHARING_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(applicationAccessService, never()).checkAndLogApartmentSharingAccess(any(), any());
        }

        @Test
        void view_callsAccessServiceWithCorrectArguments() {
            UserPrincipal principal = supportPrincipal();
            ApartmentSharing apartmentSharing = apartmentSharing(APARTMENT_SHARING_ID);
            Tenant tenant = new Tenant();
            tenant.setApartmentSharing(apartmentSharing);
            tenant.setDocuments(List.of());
            tenant.setGuarantors(List.of());
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));
            when(apartmentSharingLinkService.getFilteredLinks(apartmentSharing)).thenReturn(List.of());
            when(tenantService.findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(tenant));
            when(userApiService.getAllPartners()).thenReturn(List.of());

            String view = controller.view(new ExtendedModelMap(), APARTMENT_SHARING_ID, principal);

            assertThat(view).isEqualTo("bo/apartment-sharing-view");
            verify(applicationAccessService).checkAndLogApartmentSharingAccess(principal, APARTMENT_SHARING_ID);
        }
    }

    @Nested
    class ApartmentSharingLinkEndpoints {

        private static final Long APARTMENT_SHARING_ID = 99L;
        private static final Long LINK_ID = 7L;

        @Test
        void deleteToken_whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            ApartmentSharing apartmentSharing = apartmentSharing(APARTMENT_SHARING_ID);
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkApartmentSharingAccess(principal, APARTMENT_SHARING_ID);

            assertThatThrownBy(() -> controller.deleteToken(APARTMENT_SHARING_ID, LINK_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(apartmentSharingLinkService, never()).delete(any());
        }

        @Test
        void deleteToken_whenAuthorized_deletesLinkAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            ApartmentSharing apartmentSharing = apartmentSharing(APARTMENT_SHARING_ID);
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));

            String result = controller.deleteToken(APARTMENT_SHARING_ID, LINK_ID, principal);

            assertThat(result).isEqualTo("redirect:/bo/colocation/99");
            verify(applicationAccessService).checkApartmentSharingAccess(principal, APARTMENT_SHARING_ID);
            verify(apartmentSharingLinkService).delete(LINK_ID);
        }

        @Test
        void updateTokenStatus_whenApartmentSharingDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = supportPrincipal();
            when(applicationSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.updateTokenStatus(LINK_ID, APARTMENT_SHARING_ID, true, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(applicationAccessService, never()).checkApartmentSharingAccess(any(), any());
        }
    }

    private ApartmentSharing apartmentSharing(Long id) {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(id);
        return apartmentSharing;
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(10L, "operator", "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(20L, "support", "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
    }
}
