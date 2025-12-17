package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.UserApiRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.dossierfacile.common.constants.PartnerConstants.DF_OWNER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApartmentSharingLinkServiceTest {

    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private UserApiRepository userApiRepository;
    private ApartmentSharingLinkService service;

    private ApartmentSharing apartmentSharing;
    private static final Long DF_OWNER_ID = 100L;
    private static final Long OTHER_PARTNER_ID = 200L;

    @BeforeEach
    void setUp() {
        apartmentSharingLinkRepository = mock(ApartmentSharingLinkRepository.class);
        LinkLogService linkLogService = mock(LinkLogService.class);
        TenantCommonRepository tenantCommonRepository = mock(TenantCommonRepository.class);
        userApiRepository = mock(UserApiRepository.class);

        service = new ApartmentSharingLinkService(
                apartmentSharingLinkRepository,
                linkLogService,
                tenantCommonRepository,
                userApiRepository
        );

        apartmentSharing = ApartmentSharing.builder().id(1L).build();

        // Setup default mocks
        when(linkLogService.getFirstAndLastVisit(any(), any()))
                .thenReturn(new LinkLogServiceImpl.FirstAndLastVisit(Optional.empty(), Optional.empty()));
        when(linkLogService.countVisits(any(), any())).thenReturn(0L);
    }

    @Test
    void should_return_all_links_when_no_partners_exist() {
        // Given
        ApartmentSharingLink link1 = createLink(1L, null, false);
        ApartmentSharingLink link2 = createLink(2L, null, false);

        when(apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing))
                .thenReturn(List.of(link1, link2));
        when(userApiRepository.findByName(DF_OWNER_NAME)).thenReturn(Optional.empty());

        // When
        List<ApartmentSharingLinkModel> result = service.getLinks(apartmentSharing);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void should_filter_out_df_owner_partner_links() {
        // Given
        UserApi dfOwnerPartner = UserApi.builder().id(DF_OWNER_ID).name(DF_OWNER_NAME).build();

        ApartmentSharingLink noPartnerLink = createLink(1L, null, false);
        ApartmentSharingLink dfOwnerLink = createLink(2L, DF_OWNER_ID, true);
        ApartmentSharingLink otherPartnerLink = createLink(3L, OTHER_PARTNER_ID, true);

        when(apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing))
                .thenReturn(List.of(noPartnerLink, dfOwnerLink, otherPartnerLink));
        when(userApiRepository.findByName(DF_OWNER_NAME)).thenReturn(Optional.of(dfOwnerPartner));

        // When
        List<ApartmentSharingLinkModel> result = service.getLinks(apartmentSharing);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApartmentSharingLinkModel::getId)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void should_return_full_data_partner_links_that_are_not_df_owner() {
        // Given
        UserApi dfOwnerPartner = UserApi.builder().id(DF_OWNER_ID).name(DF_OWNER_NAME).build();

        ApartmentSharingLink partnerLink1 = createLink(1L, 201L, true);
        ApartmentSharingLink partnerLink2 = createLink(2L, 202L, true);
        ApartmentSharingLink dfOwnerLink = createLink(3L, DF_OWNER_ID, true);

        when(apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing))
                .thenReturn(List.of(partnerLink1, partnerLink2, dfOwnerLink));
        when(userApiRepository.findByName(DF_OWNER_NAME)).thenReturn(Optional.of(dfOwnerPartner));

        // When
        List<ApartmentSharingLinkModel> result = service.getLinks(apartmentSharing);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApartmentSharingLinkModel::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void should_handle_when_df_owner_partner_does_not_exist_in_database() {
        // Given
        ApartmentSharingLink partnerLink1 = createLink(1L, 201L, true);
        ApartmentSharingLink partnerLink2 = createLink(2L, 202L, true);

        when(apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing))
                .thenReturn(List.of(partnerLink1, partnerLink2));
        when(userApiRepository.findByName(DF_OWNER_NAME)).thenReturn(Optional.empty());

        // When
        List<ApartmentSharingLinkModel> result = service.getLinks(apartmentSharing);

        // Then - All partner links should be returned since DF_OWNER doesn't exist
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApartmentSharingLinkModel::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void should_not_include_non_full_data_partner_links() {
        // Given
        UserApi dfOwnerPartner = UserApi.builder().id(DF_OWNER_ID).name(DF_OWNER_NAME).build();

        ApartmentSharingLink noPartnerLink = createLink(1L, null, false);
        ApartmentSharingLink nonFullDataPartnerLink = createLink(2L, OTHER_PARTNER_ID, false);
        ApartmentSharingLink fullDataPartnerLink = createLink(3L, OTHER_PARTNER_ID, true);

        when(apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing))
                .thenReturn(List.of(noPartnerLink, nonFullDataPartnerLink, fullDataPartnerLink));
        when(userApiRepository.findByName(DF_OWNER_NAME)).thenReturn(Optional.of(dfOwnerPartner));

        // When
        List<ApartmentSharingLinkModel> result = service.getLinks(apartmentSharing);

        // Then - Only no-partner link and full-data partner link should be included
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApartmentSharingLinkModel::getId)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    private ApartmentSharingLink createLink(Long id, Long partnerId, boolean fullData) {
        return ApartmentSharingLink.builder()
                .id(id)
                .token(UUID.randomUUID())
                .partnerId(partnerId)
                .fullData(fullData)
                .creationDate(LocalDateTime.now())
                .linkType(ApartmentSharingLinkType.LINK)
                .disabled(false)
                .deleted(false)
                .build();
    }
}
