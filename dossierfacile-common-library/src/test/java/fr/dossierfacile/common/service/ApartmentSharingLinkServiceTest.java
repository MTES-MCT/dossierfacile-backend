package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.repository.UserApiRepository;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.dossierfacile.common.constants.PartnerConstants.DF_OWNER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class ApartmentSharingLinkServiceTest {

    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private UserApiRepository userApiRepository;
    private TenantUserApiRepository tenantUserApiRepository;
    private PartnerCallBackService partnerCallBackService;
    private KeycloakCommonService keycloakCommonService;
    private MailCommonService mailCommonService;
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
        tenantUserApiRepository = mock(TenantUserApiRepository.class);
        partnerCallBackService = mock(PartnerCallBackService.class);
        keycloakCommonService = mock(KeycloakCommonService.class);
        mailCommonService = mock(MailCommonService.class);
        LogService logService = mock(LogService.class);

        service = new ApartmentSharingLinkService(
                apartmentSharingLinkRepository,
                linkLogService,
                tenantCommonRepository,
                userApiRepository,
                tenantUserApiRepository,
                partnerCallBackService,
                keycloakCommonService,
                mailCommonService,
                logService
        );

        apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .apartmentSharingLinks(new ArrayList<>())
                .build();

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
                .apartmentSharing(apartmentSharing)
                .disabled(false)
                .deleted(false)
                .build();
    }

    @Test
    void should_delete_owner_link_and_associated_df_owner_partner_links() {
        // Given
        Long ownerLinkId = 1L;
        UserApi dfOwnerPartner = UserApi.builder().id(DF_OWNER_ID).name(DF_OWNER_NAME).build();

        ApartmentSharingLink ownerLink = createOwnerLink(ownerLinkId);
        ApartmentSharingLink dfOwnerPartnerLink1 = createPartnerLink(2L, DF_OWNER_ID, true);
        ApartmentSharingLink dfOwnerPartnerLink2 = createPartnerLink(3L, DF_OWNER_ID, false);
        ApartmentSharingLink otherPartnerLink = createPartnerLink(4L, OTHER_PARTNER_ID, true);

        when(apartmentSharingLinkRepository.findById(ownerLinkId))
                .thenReturn(Optional.of(ownerLink));
        when(userApiRepository.findByName(DF_OWNER_NAME))
                .thenReturn(Optional.of(dfOwnerPartner));

        // When
        service.delete(ownerLinkId);

        // Then
        // Verify all DF_OWNER links are deleted
        assertThat(ownerLink.isDeleted()).isTrue();
        assertThat(dfOwnerPartnerLink1.isDeleted()).isTrue();
        assertThat(dfOwnerPartnerLink2.isDeleted()).isTrue();
        assertThat(otherPartnerLink.isDeleted()).isFalse();

        // Verify NO OAuth revocation happened
        verify(tenantUserApiRepository, never()).findAllByApartmentSharingAndUserApi(any(), any());
        verify(keycloakCommonService, never()).revokeUserConsent(any(), any());
    }

    @Test
    void should_delete_partner_link_and_revoke_oauth_access() {
        // Given
        Long partnerLinkId = 1L;
        Long partnerId = 200L;

        ApartmentSharingLink partnerLinkFull = createPartnerLink(partnerLinkId, partnerId, true);
        ApartmentSharingLink partnerLinkLight = createPartnerLink(2L, partnerId, false);
        ApartmentSharingLink otherPartnerLink = createPartnerLink(3L, 300L, true);

        Tenant tenant1 = createTenant(10L);
        Tenant tenant2 = createTenant(11L);
        UserApi userApi = createUserApi(partnerId);
        TenantUserApi tenantUserApi1 = createTenantUserApi(tenant1, userApi);
        TenantUserApi tenantUserApi2 = createTenantUserApi(tenant2, userApi);

        when(apartmentSharingLinkRepository.findById(partnerLinkId))
                .thenReturn(Optional.of(partnerLinkFull));
        when(tenantUserApiRepository.findAllByApartmentSharingAndUserApi(apartmentSharing.getId(), partnerId))
                .thenReturn(List.of(tenantUserApi1, tenantUserApi2));

        // When
        service.delete(partnerLinkId);

        // Then
        // OAuth revocation happened for all tenants
        verify(tenantUserApiRepository).delete(tenantUserApi1);
        verify(tenantUserApiRepository).delete(tenantUserApi2);
        verify(keycloakCommonService).revokeUserConsent(tenant1, userApi);
        verify(keycloakCommonService).revokeUserConsent(tenant2, userApi);
        verify(partnerCallBackService).sendRevokedAccessCallback(tenant1, userApi);
        verify(partnerCallBackService).sendRevokedAccessCallback(tenant2, userApi);
        verify(mailCommonService).sendEmailPartnerAccessRevoked(tenant1, userApi, tenant1);
        verify(mailCommonService).sendEmailPartnerAccessRevoked(tenant2, userApi, tenant2);

        // Both partner links (full and light) deleted
        assertThat(partnerLinkFull.isDeleted()).isTrue();
        assertThat(partnerLinkLight.isDeleted()).isTrue();
        assertThat(otherPartnerLink.isDeleted()).isFalse();
    }

    @Test
    void should_delete_simple_link_without_revocation() {
        // Given
        Long linkId = 1L;
        ApartmentSharingLink simpleLink = createLink(linkId, null, false);
        ApartmentSharingLink otherLink = createLink(2L, null, false);

        apartmentSharing.getApartmentSharingLinks().add(simpleLink);
        apartmentSharing.getApartmentSharingLinks().add(otherLink);

        when(apartmentSharingLinkRepository.findById(linkId))
                .thenReturn(Optional.of(simpleLink));

        // When
        service.delete(linkId);

        // Then
        assertThat(simpleLink.isDeleted()).isTrue();
        assertThat(otherLink.isDeleted()).isFalse();

        // No OAuth revocation
        verify(tenantUserApiRepository, never()).findAllByApartmentSharingAndUserApi(any(), any());
    }

    private ApartmentSharingLink createOwnerLink(Long id) {
        ApartmentSharingLink link = ApartmentSharingLink.builder()
                .id(id)
                .token(UUID.randomUUID())
                .linkType(ApartmentSharingLinkType.OWNER)
                .apartmentSharing(apartmentSharing)
                .creationDate(LocalDateTime.now())
                .disabled(false)
                .deleted(false)
                .build();
        apartmentSharing.getApartmentSharingLinks().add(link);
        return link;
    }

    private ApartmentSharingLink createPartnerLink(Long id, Long partnerId, boolean fullData) {
        ApartmentSharingLink link = ApartmentSharingLink.builder()
                .id(id)
                .token(UUID.randomUUID())
                .partnerId(partnerId)
                .fullData(fullData)
                .linkType(ApartmentSharingLinkType.PARTNER)
                .apartmentSharing(apartmentSharing)
                .creationDate(LocalDateTime.now())
                .disabled(false)
                .deleted(false)
                .build();
        apartmentSharing.getApartmentSharingLinks().add(link);
        return link;
    }

    private Tenant createTenant(Long id) {
        return Tenant.builder()
                .id(id)
                .apartmentSharing(apartmentSharing)
                .build();
    }

    private UserApi createUserApi(Long id) {
        return UserApi.builder()
                .id(id)
                .name("partner-" + id)
                .build();
    }

    private TenantUserApi createTenantUserApi(Tenant tenant, UserApi userApi) {
        return TenantUserApi.builder()
                .tenant(tenant)
                .userApi(userApi)
                .build();
    }

    @Test
    void should_create_new_link_when_no_valid_link_exists() {
        // Given
        Tenant tenant = createTenant(10L);
        boolean fullData = true;

        when(apartmentSharingLinkRepository.findValidDefaultLinks(apartmentSharing.getId(), fullData, tenant.getId()))
                .thenReturn(List.of());

        // Mock the save to return a link with ID
        when(apartmentSharingLinkRepository.save(any(ApartmentSharingLink.class)))
                .thenAnswer(invocation -> {
                    ApartmentSharingLink link = invocation.getArgument(0);
                    link.setId(999L); // Simulate DB-generated ID
                    return link;
                });

        // When
        ApartmentSharingLinkModel result = service.getDefaultLink(apartmentSharing, tenant, fullData);

        // Then
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.isFullData()).isTrue();
    }

    @Test
    void should_return_existing_link_when_expiration_is_more_than_10_days() {
        // Given
        Tenant tenant = createTenant(10L);
        boolean fullData = true;
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(15);

        ApartmentSharingLink existingLink = ApartmentSharingLink.builder()
                .id(100L)
                .token(UUID.randomUUID())
                .apartmentSharing(apartmentSharing)
                .fullData(fullData)
                .createdBy(tenant.getId())
                .expirationDate(expirationDate)
                .linkType(ApartmentSharingLinkType.LINK)
                .disabled(false)
                .deleted(false)
                .creationDate(LocalDateTime.now())
                .build();

        when(apartmentSharingLinkRepository.findValidDefaultLinks(apartmentSharing.getId(), fullData, tenant.getId()))
                .thenReturn(List.of(existingLink));

        // When
        ApartmentSharingLinkModel result = service.getDefaultLink(apartmentSharing, tenant, fullData);

        // Then
        verify(apartmentSharingLinkRepository, never()).save(any(ApartmentSharingLink.class));
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.isFullData()).isTrue();
    }

    @Test
    void should_create_new_link_when_expiration_is_10_days_or_less() {
        // Given
        Tenant tenant = createTenant(10L);
        boolean fullData = true;
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(10);

        ApartmentSharingLink existingLink = ApartmentSharingLink.builder()
                .id(100L)
                .token(UUID.randomUUID())
                .apartmentSharing(apartmentSharing)
                .fullData(fullData)
                .createdBy(tenant.getId())
                .expirationDate(expirationDate)
                .linkType(ApartmentSharingLinkType.LINK)
                .disabled(false)
                .deleted(false)
                .creationDate(LocalDateTime.now())
                .build();

        when(apartmentSharingLinkRepository.findValidDefaultLinks(apartmentSharing.getId(), fullData, tenant.getId()))
                .thenReturn(List.of(existingLink));

        // Mock the save to return a link with ID
        when(apartmentSharingLinkRepository.save(any(ApartmentSharingLink.class)))
                .thenAnswer(invocation -> {
                    ApartmentSharingLink link = invocation.getArgument(0);
                    link.setId(999L); // Simulate DB-generated ID
                    return link;
                });

        // When
        ApartmentSharingLinkModel result = service.getDefaultLink(apartmentSharing, tenant, fullData);

        // Then
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getId()).isNotEqualTo(100L); // New link created
        assertThat(result.isFullData()).isTrue();
    }

    @Test
    void should_select_link_with_furthest_expiration_date() {
        // Given
        Tenant tenant = createTenant(10L);
        boolean fullData = true;

        ApartmentSharingLink linkExpiring15Days = ApartmentSharingLink.builder()
                .id(100L)
                .token(UUID.randomUUID())
                .apartmentSharing(apartmentSharing)
                .fullData(fullData)
                .createdBy(tenant.getId())
                .expirationDate(LocalDateTime.now().plusDays(15))
                .linkType(ApartmentSharingLinkType.LINK)
                .disabled(false)
                .deleted(false)
                .creationDate(LocalDateTime.now())
                .build();

        ApartmentSharingLink linkExpiring30Days = ApartmentSharingLink.builder()
                .id(200L)
                .token(UUID.randomUUID())
                .apartmentSharing(apartmentSharing)
                .fullData(fullData)
                .createdBy(tenant.getId())
                .expirationDate(LocalDateTime.now().plusDays(30))
                .linkType(ApartmentSharingLinkType.LINK)
                .disabled(false)
                .deleted(false)
                .creationDate(LocalDateTime.now())
                .build();

        // Repository returns sorted by expiration DESC, so 30 days first
        when(apartmentSharingLinkRepository.findValidDefaultLinks(apartmentSharing.getId(), fullData, tenant.getId()))
                .thenReturn(List.of(linkExpiring30Days, linkExpiring15Days));

        // When
        ApartmentSharingLinkModel result = service.getDefaultLink(apartmentSharing, tenant, fullData);

        // Then
        verify(apartmentSharingLinkRepository, never()).save(any(ApartmentSharingLink.class));
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L); // Link with 30 days selected
    }
}
