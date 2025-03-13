package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMailImpl;
import fr.dossierfacile.common.mapper.mail.UserApiMapperForMail;
import fr.dossierfacile.common.mapper.mail.UserApiMapperForMailImpl;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserServiceImplTest {

    private static final UserRepository userRepository = mock(UserRepository.class);
    private static final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository = mock(PasswordRecoveryTokenRepository.class);
    private static final MailService mailService = mock(MailService.class);
    private static final PasswordRecoveryTokenService passwordRecoveryTokenService = mock(PasswordRecoveryTokenService.class);
    // We have to mock this mapper there is too much logic inside
    private static final TenantMapper tenantMapper = mock(TenantMapper.class);
    private static final TenantCommonRepository tenantRepository = mock(TenantCommonRepository.class);
    private static final LogService logService = mock(LogService.class);
    private static final KeycloakService keycloakService = mock(KeycloakService.class);
    private static final UserApiService userApiService = mock(UserApiService.class);
    private static final PartnerCallBackService partnerCallBackService = mock(PartnerCallBackService.class);
    private static final ApartmentSharingService apartmentSharingService = mock(ApartmentSharingService.class);
    private static final TenantCommonService tenantCommonService = mock(TenantCommonService.class);

    @Autowired
    private UserService userService;

    @TestConfiguration
    static class UserServiceTestConfiguration {

        @Bean
        public TenantMapperForMail tenantMapperForMail() {
            return new TenantMapperForMailImpl();
        }

        @Bean
        public UserApiMapperForMail userApiMapperForMail() {
            return new UserApiMapperForMailImpl();
        }

        @Bean
        public UserService userService(TenantMapperForMailImpl tenantMapperForMail) {
            return new UserServiceImpl(userRepository, passwordRecoveryTokenRepository, mailService, passwordRecoveryTokenService, tenantMapper, tenantRepository, logService, keycloakService, userApiService, partnerCallBackService, apartmentSharingService, tenantCommonService, tenantMapperForMail);
        }
    }

    @BeforeEach
    void before() {
        reset(userRepository, passwordRecoveryTokenRepository, mailService, passwordRecoveryTokenService, tenantMapper, tenantRepository, logService, keycloakService, userApiService, partnerCallBackService, apartmentSharingService, tenantCommonService);
    }

    @Nested
    class CreatePasswordTest {

        @Test
        void shouldCreatePasswordForUser() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();
            userService.createPassword(tenant, "password");

            verify(keycloakService, times(1)).createKeyCloakPassword(tenant.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(tenant.getId()), null);
        }

        @Test
        void shouldThrowPasswordRecoveryTokenNotFoundExceptionWhenCreatePasswordForToken() {
            var token = "test";
            doThrow(new PasswordRecoveryTokenNotFoundException(token)).when(passwordRecoveryTokenRepository).findByToken(token);

            var exception = assertThrows(PasswordRecoveryTokenNotFoundException.class, () -> userService.createPassword(token, "password"));
            assertEquals(exception.getMessage(), "Could not find password recovery token or is expired " + token);
        }

        @Test
        void shouldCreatePassword() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .keycloakId("keycloakId")
                    .build();

            var passwordRecoveryToken = PasswordRecoveryToken.builder()
                    .id(1L)
                    .token("token")
                    .user(tenant)
                    .build();

            when(passwordRecoveryTokenRepository.findByToken("token")).thenReturn(Optional.of(passwordRecoveryToken));
            when(keycloakService.getKeycloakId(tenant.getEmail())).thenReturn(tenant.getKeycloakId());


            userService.createPassword("token", "password");

            verify(userRepository, times(0)).save(tenant);
            verify(keycloakService, times(1)).createKeyCloakPassword(tenant.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(tenant.getId()), null);
            verify(passwordRecoveryTokenRepository, times(1)).delete(passwordRecoveryToken);


        }

        @Test
        void shouldCreatePasswordAndUpdateTheUser() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .keycloakId("keycloakId")
                    .build();

            var passwordRecoveryToken = PasswordRecoveryToken.builder()
                    .id(1L)
                    .token("token")
                    .user(tenant)
                    .build();

            when(passwordRecoveryTokenRepository.findByToken("token")).thenReturn(Optional.of(passwordRecoveryToken));
            when(keycloakService.getKeycloakId(tenant.getEmail())).thenReturn("keycloakId2");

            userService.createPassword("token", "password");

            verify(userRepository, times(1)).save(tenant);
            verify(keycloakService, times(1)).createKeyCloakPassword(tenant.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(tenant.getId()), null);
            verify(passwordRecoveryTokenRepository, times(1)).delete(passwordRecoveryToken);

        }

    }

    @Nested
    class ForgotPasswordTest {

        @Test
        void shouldSendResetPasswordEmail() {

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            var passwordRecoveryToken = PasswordRecoveryToken.builder()
                    .id(1L)
                    .token("token")
                    .user(tenant)
                    .build();

            when(tenantRepository.findByEmail("test@test.fr")).thenReturn(Optional.of(tenant));
            when(passwordRecoveryTokenService.create(tenant)).thenReturn(passwordRecoveryToken);
            userService.forgotPassword("test@test.fr");

            verify(passwordRecoveryTokenService, times(1)).create(tenant);
            verify(mailService, times(1)).sendEmailNewPassword(tenant, passwordRecoveryToken);

        }

        @Test
        void shouldThrowUserNotFoundWhenResetPasswordEmail() {
            var email = "test@test.fr";
            doThrow(new UserNotFoundException(email)).when(tenantRepository).findByEmail(email);

            var exception = assertThrows(UserNotFoundException.class, () -> userService.forgotPassword(email));

            assertEquals(exception.getMessage(), "Could not find user with email " + email);
        }
    }


    @Nested
    class DeleteAccountTest {

        @BeforeEach
        void before() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void after() {
            TransactionSynchronizationManager.clear();
        }

        @Test
        void shouldDeleteAccountWithEmptyApartmentSharingAndWithKeycloakId() {

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .keycloakId("keycloakId")
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(List.of(tenant));

            userService.deleteAccount(tenant);

            verify(logService, times(1)).saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);
            verify(tenantCommonService, times(1)).deleteTenantData(tenant);
            verify(userRepository, times(1)).delete(tenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, tenant);
            verify(partnerCallBackService, times(0)).getWebhookDTO(any(), any(), any());

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
                try {
                    synchronization.afterCommit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> tenant.getEmail().equals(tenantDto.getEmail())));
            verify(keycloakService, times(1)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountWithEmptyApartmentSharingAndWithoutKeycloakId() {
            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(List.of(tenant));

            userService.deleteAccount(tenant);

            verify(logService, times(1)).saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);
            verify(tenantCommonService, times(1)).deleteTenantData(tenant);
            verify(userRepository, times(1)).delete(tenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, tenant);
            verify(partnerCallBackService, times(0)).getWebhookDTO(any(), any(), any());

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
                try {
                    synchronization.afterCommit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> tenant.getEmail().equals(tenantDto.getEmail())));
            verify(keycloakService, times(0)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountAndCoTenantAccountsKeycloakId() {

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var mainTenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();

            var coTenant = Tenant.builder()
                    .id(2L)
                    .email("test2@test.fr")
                    .tenantType(TenantType.JOIN)
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(List.of(mainTenant, coTenant));

            userService.deleteAccount(mainTenant);

            verify(logService, times(2)).saveLogWithTenantData(eq(LogType.ACCOUNT_DELETE), any(Tenant.class));
            verify(logService).saveLogWithTenantData(LogType.ACCOUNT_DELETE, mainTenant);
            verify(logService).saveLogWithTenantData(LogType.ACCOUNT_DELETE, coTenant);

            verify(tenantCommonService, times(2)).deleteTenantData(any(Tenant.class));
            verify(tenantCommonService).deleteTenantData(mainTenant);
            verify(tenantCommonService).deleteTenantData(coTenant);

            verify(userRepository, times(1)).delete(coTenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, coTenant);

            verify(apartmentSharingService, times(1)).delete(mainTenant.getApartmentSharing());

            verify(partnerCallBackService, times(0)).getWebhookDTO(any(), any(), any());

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
                try {
                    synchronization.afterCommit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(mailService, times(2)).sendEmailAccountDeleted(any());
            verify(mailService).sendEmailAccountDeleted(argThat(tenantDto -> mainTenant.getEmail().equals(tenantDto.getEmail())));
            verify(mailService).sendEmailAccountDeleted(argThat(tenantDto -> coTenant.getEmail().equals(tenantDto.getEmail())));
            verify(keycloakService, times(0)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountAndCallWebhookIntegrations() {

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .apartmentSharing(apartmentSharing)
                    .build();

            var userApi1 = UserApi.builder()
                    .id(1L)
                    .build();

            var userApi2 = UserApi.builder()
                    .id(2L)
                    .build();

            var tenantUserApi = TenantUserApi.builder()
                    .id(new TenantUserApiKey(tenant.getId(), userApi1.getId()))
                    .tenant(tenant)
                    .userApi(userApi1)
                    .build();

            var tenantUserApi2 = TenantUserApi.builder()
                    .id(new TenantUserApiKey(tenant.getId(), userApi2.getId()))
                    .tenant(tenant)
                    .userApi(userApi2)
                    .build();

            tenant.setTenantsUserApi(List.of(
                    tenantUserApi,
                    tenantUserApi2
            ));

            var applicationModel1 = ApplicationModel.builder()
                    .id(1L)
                    .build();

            var applicationModel2 = ApplicationModel.builder()
                    .id(2L)
                    .build();

            when(partnerCallBackService.getWebhookDTO(tenant, userApi1, PartnerCallBackType.DELETED_ACCOUNT)).thenReturn(applicationModel1);
            when(partnerCallBackService.getWebhookDTO(tenant, userApi2, PartnerCallBackType.DELETED_ACCOUNT)).thenReturn(applicationModel2);

            apartmentSharing.setTenants(List.of(tenant));

            userService.deleteAccount(tenant);

            verify(logService, times(1)).saveLogWithTenantData(LogType.ACCOUNT_DELETE, tenant);
            verify(tenantCommonService, times(1)).deleteTenantData(tenant);
            verify(userRepository, times(1)).delete(tenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, tenant);
            verify(partnerCallBackService, times(2)).getWebhookDTO(any(), any(), any());
            verify(partnerCallBackService).getWebhookDTO(tenant, tenantUserApi.getUserApi(), PartnerCallBackType.DELETED_ACCOUNT);
            verify(partnerCallBackService).getWebhookDTO(tenant, tenantUserApi2.getUserApi(), PartnerCallBackType.DELETED_ACCOUNT);
            verify(partnerCallBackService, times(2)).sendCallBack(any(), any(), any());
            verify(partnerCallBackService).sendCallBack(tenant, userApi1, applicationModel1);
            verify(partnerCallBackService).sendCallBack(tenant, userApi2, applicationModel2);

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
                try {
                    synchronization.afterCommit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> tenant.getEmail().equals(tenantDto.getEmail())));
            verify(keycloakService, times(0)).deleteKeycloakUserById("keycloakId");
        }

    }

    @Nested
    class DeleteCoTenantTest {

        @BeforeEach
        void before() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void after() {
            TransactionSynchronizationManager.clear();
        }

        @Test
        void shouldReturnFalseWhenTenantIsNotCreate() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.JOIN)
                    .build();

            var result = userService.deleteCoTenant(tenant, 2L);
            assertEquals(false, result);
        }

        @Test
        void shouldReturnFalseWhenCoTenantIsNotPresent() {

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(List.of(tenant));

            var result = userService.deleteCoTenant(tenant, 2L);
            assertEquals(false, result);
        }

        @Test
        void shouldDeleteCoTenant() {

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();

            var coTenant = Tenant.builder()
                    .id(2L)
                    .email("test2@test.fr")
                    .tenantType(TenantType.JOIN)
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setTenants(List.of(tenant, coTenant));

            var result = userService.deleteCoTenant(tenant, coTenant.getId());

            assertEquals(true, result);
            verify(userRepository, times(1)).delete(coTenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, coTenant);

        }

    }

    @Nested
    class LinkTenantToPartnerTest {

        @Test
        void shouldDoNothingIfPartnerDoesNotExist() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            when(userApiService.findByName("partner")).thenReturn(Optional.empty());

            userService.linkTenantToPartner(tenant, "partner", "internalPartnerId");

            verify(partnerCallBackService, times(0)).registerTenant(any(), any());
        }

        @Test
        void shouldLinkTenantToPartnerWhenNotCouple() {
            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .applicationType(ApplicationType.ALONE)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();

            var userApi = UserApi.builder()
                    .id(1L)
                    .build();

            apartmentSharing.setTenants(List.of(tenant));

            when(userApiService.findByName("partner")).thenReturn(Optional.of(userApi));

            userService.linkTenantToPartner(tenant, "partner", "internalPartnerId");

            verify(partnerCallBackService, times(1)).registerTenant(tenant, userApi);
        }

        @Test
        void shouldLinkTenantToPartnerWhenCouple() {
            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .applicationType(ApplicationType.COUPLE)
                    .build();

            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();

            var tenant2 = Tenant.builder()
                    .id(2L)
                    .email("test2@test.fr")
                    .tenantType(TenantType.JOIN)
                    .apartmentSharing(apartmentSharing)
                    .build();

            var userApi = UserApi.builder()
                    .id(1L)
                    .build();

            apartmentSharing.setTenants(List.of(tenant, tenant2));

            when(userApiService.findByName("partner")).thenReturn(Optional.of(userApi));

            userService.linkTenantToPartner(tenant, "partner", "internalPartnerId");

            verify(partnerCallBackService, times(2)).registerTenant(any(), any());
            verify(partnerCallBackService).registerTenant(tenant, userApi);
            verify(partnerCallBackService).registerTenant(tenant2, userApi);
        }
    }

    @Test
    void logoutTest() {
        userService.logout("keycloakId");

        verify(keycloakService, times(1)).logout("keycloakId");
    }

    @Nested
    class UnlinkFranceConnectTest {

        @Test
        void shouldThrowIllegalArgumentExceptionWhenTenantNotFound() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            when(userRepository.findById(tenant.getId())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    userService.unlinkFranceConnect(tenant));
        }

        @Test
        void shouldUnlinkFranceConnect() {
            var tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .build();

            when(userRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

            userService.unlinkFranceConnect(tenant);

            verify(userRepository, times(1)).save(tenant);
            verify(keycloakService, times(1)).unlinkFranceConnect(tenant);

        }

    }
}
