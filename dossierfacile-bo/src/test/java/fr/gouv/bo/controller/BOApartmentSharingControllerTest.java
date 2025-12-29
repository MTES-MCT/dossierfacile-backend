package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.LinkLogServiceImpl.FirstAndLastVisit;
import fr.gouv.bo.dto.ApartmentSharingLinkEnrichedDTO;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOApartmentSharingControllerTest {

    @Mock
    private LinkLogService linkLogService;

    @Mock
    private LinkLogRepository linkLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserApiService userApiService;

    private BOApartmentSharingController controller;

    @BeforeEach
    void setUp() {
        controller = new BOApartmentSharingController(
                null, null, userApiService, null, linkLogService, linkLogRepository, userService
        );
        ReflectionTestUtils.setField(controller, "tenantBaseUrl", "https://example.com");
    }

    @Test
    void enrichApartmentSharingLinks_shouldSkipDeletedLinks() throws Exception {
        // Given
        ApartmentSharingLink deletedLink = createLink(1L, ApartmentSharingLinkType.LINK, true);

        // When
        List<ApartmentSharingLinkEnrichedDTO> result = invokeEnrichMethod(List.of(deletedLink));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void enrichApartmentSharingLinks_shouldFilterAccessLogsByVisitTypes() throws Exception {
        // Given
        ApartmentSharingLink link = createLink(1L, ApartmentSharingLinkType.LINK, false);
        
        LinkLog visitLog1 = createLinkLog(LinkType.FULL_APPLICATION);
        LinkLog visitLog2 = createLinkLog(LinkType.DOCUMENT);
        LinkLog nonVisitLog = createLinkLog(LinkType.ENABLED_LINK);

        when(linkLogService.getFirstAndLastVisit(any(UUID.class), any(ApartmentSharing.class)))
                .thenReturn(new FirstAndLastVisit(Optional.empty(), Optional.empty()));
        when(linkLogService.countVisits(any(UUID.class), any(ApartmentSharing.class)))
                .thenReturn(0L);
        when(linkLogRepository.findByApartmentSharingAndToken(any(ApartmentSharing.class), any(UUID.class)))
                .thenReturn(List.of(visitLog1, visitLog2, nonVisitLog));

        // When
        List<ApartmentSharingLinkEnrichedDTO> result = invokeEnrichMethod(List.of(link));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAccessLogs()).hasSize(2)
                .extracting(LinkLog::getLinkType)
                .containsExactlyInAnyOrder(LinkType.FULL_APPLICATION, LinkType.DOCUMENT);
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

    private LinkLog createLinkLog(LinkType linkType) {
        LinkLog log = new LinkLog();
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
}
