package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.PasswordRecoveryTokenNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.repository.PasswordRecoveryTokenRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.common.dto.mail.TenantDto;
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
public class UserServiceImplTest {

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
            var user = new Tenant();
            user.setId(1L);
            user.setEmail("test@test.fr");
            userService.createPassword(user, "password");

            verify(keycloakService, times(1)).createKeyCloakPassword(user.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(user.getId()), null);
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
            var user = new Tenant();
            user.setId(1L);
            user.setEmail("test@test.fr");
            user.setKeycloakId("keycloakId");

            var passwordRecoveryToken = new PasswordRecoveryToken();
            passwordRecoveryToken.setId(1L);
            passwordRecoveryToken.setToken("token");
            passwordRecoveryToken.setUser(user);

            when(passwordRecoveryTokenRepository.findByToken("token")).thenReturn(Optional.of(passwordRecoveryToken));
            when(keycloakService.getKeycloakId(user.getEmail())).thenReturn(user.getKeycloakId());


            userService.createPassword("token", "password");

            verify(userRepository, times(0)).save(user);
            verify(keycloakService, times(1)).createKeyCloakPassword(user.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(user.getId()), null);
            verify(passwordRecoveryTokenRepository, times(1)).delete(passwordRecoveryToken);


        }

        @Test
        void shouldCreatePasswordAndUpdateTheUser() {
            var user = new Tenant();
            user.setId(1L);
            user.setEmail("test@test.fr");
            user.setKeycloakId("keycloakId");

            var passwordRecoveryToken = new PasswordRecoveryToken();
            passwordRecoveryToken.setId(1L);
            passwordRecoveryToken.setToken("token");
            passwordRecoveryToken.setUser(user);

            when(passwordRecoveryTokenRepository.findByToken("token")).thenReturn(Optional.of(passwordRecoveryToken));
            when(keycloakService.getKeycloakId(user.getEmail())).thenReturn("keycloakId2");

            userService.createPassword("token", "password");

            verify(userRepository, times(1)).save(user);
            verify(keycloakService, times(1)).createKeyCloakPassword(user.getKeycloakId(), "password");
            verify(tenantMapper, times(1)).toTenantModel(tenantRepository.getReferenceById(user.getId()), null);
            verify(passwordRecoveryTokenRepository, times(1)).delete(passwordRecoveryToken);

        }

    }

    @Nested
    class ForgotPasswordTest {

        @Test
        void shouldSendResetPasswordEmail() {

            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            var passwordRecoveryToken = new PasswordRecoveryToken();
            passwordRecoveryToken.setUser(tenant);
            passwordRecoveryToken.setId(1L);
            passwordRecoveryToken.setToken("token");

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
            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);

            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test");
            tenant.setKeycloakId("keycloakId");
            tenant.setApartmentSharing(apartmentSharing);

            var expectedTenantDto = new TenantDto();
            expectedTenantDto.setEmail("test");

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

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> "test".equals(tenantDto.getEmail())));
            verify(keycloakService, times(1)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountWithEmptyApartmentSharingAndWithoutKeycloakId() {
            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);

            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test");
            tenant.setApartmentSharing(apartmentSharing);

            var expectedTenantDto = new TenantDto();
            expectedTenantDto.setEmail("test");

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

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> "test".equals(tenantDto.getEmail())));
            verify(keycloakService, times(0)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountAndCoTenantAccountsKeycloakId() {

            var mainTenant = new Tenant();
            mainTenant.setId(1L);
            mainTenant.setEmail("test");
            mainTenant.setTenantType(TenantType.CREATE);

            var cotenant = new Tenant();
            cotenant.setId(2L);
            cotenant.setEmail("test2");
            cotenant.setTenantType(TenantType.JOIN);

            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);
            apartmentSharing.setTenants(List.of(mainTenant, cotenant));

            mainTenant.setApartmentSharing(apartmentSharing);
            cotenant.setApartmentSharing(apartmentSharing);

            var expectedTenantDto = new TenantDto();
            expectedTenantDto.setEmail("test");

            userService.deleteAccount(mainTenant);

            verify(logService, times(2)).saveLogWithTenantData(eq(LogType.ACCOUNT_DELETE), any(Tenant.class));
            verify(logService).saveLogWithTenantData(LogType.ACCOUNT_DELETE, mainTenant);
            verify(logService).saveLogWithTenantData(LogType.ACCOUNT_DELETE, cotenant);

            verify(tenantCommonService, times(2)).deleteTenantData(any(Tenant.class));
            verify(tenantCommonService).deleteTenantData(mainTenant);
            verify(tenantCommonService).deleteTenantData(cotenant);

            verify(userRepository, times(1)).delete(cotenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, cotenant);

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
            verify(mailService).sendEmailAccountDeleted(argThat(tenantDto -> "test".equals(tenantDto.getEmail())));
            verify(mailService).sendEmailAccountDeleted(argThat(tenantDto -> "test2".equals(tenantDto.getEmail())));
            verify(keycloakService, times(0)).deleteKeycloakUserById("keycloakId");

        }

        @Test
        void shouldDeleteAccountAndCallWebhookIntegrations() {

            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);

            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test");
            tenant.setApartmentSharing(apartmentSharing);

            var userApi1 = new UserApi();
            userApi1.setId(1L);

            var userApi2 = new UserApi();
            userApi2.setId(1L);

            var tenantUserApi = new TenantUserApi();
            tenantUserApi.setId(new TenantUserApiKey(1L, 1L));
            tenantUserApi.setTenant(tenant);
            tenantUserApi.setUserApi(userApi1);

            var tenantUserApi2 = new TenantUserApi();
            tenantUserApi2.setId(new TenantUserApiKey(1L, 2L));
            tenantUserApi2.setTenant(tenant);
            tenantUserApi2.setUserApi(userApi2);

            tenant.setTenantsUserApi(List.of(
                    tenantUserApi,
                    tenantUserApi2
            ));

            var applicationModel1 = new ApplicationModel();
            applicationModel1.setId(1L);

            var applicationModel2 = new ApplicationModel();
            applicationModel2.setId(2L);

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

            verify(mailService, times(1)).sendEmailAccountDeleted(argThat(tenantDto -> "test".equals(tenantDto.getEmail())));
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
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setTenantType(TenantType.JOIN);
            var result = userService.deleteCoTenant(tenant, 2L);
            assertEquals(result, false);
        }

        @Test
        void shouldReturnFalseWhenCotenantIsNotPresent() {
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setTenantType(TenantType.CREATE);

            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);
            apartmentSharing.setTenants(List.of(tenant));

            tenant.setApartmentSharing(apartmentSharing);

            var result = userService.deleteCoTenant(tenant, 2L);
            assertEquals(result, false);
        }

        @Test
        void shouldDeleteCotenant() {
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setTenantType(TenantType.CREATE);

            var cotenant = new Tenant();
            cotenant.setId(2L);
            cotenant.setTenantType(TenantType.JOIN);

            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);
            apartmentSharing.setTenants(List.of(tenant, cotenant));

            tenant.setApartmentSharing(apartmentSharing);
            cotenant.setApartmentSharing(apartmentSharing);

            var result = userService.deleteCoTenant(tenant, cotenant.getId());

            assertEquals(result, true);
            verify(userRepository, times(1)).delete(cotenant);
            verify(apartmentSharingService, times(1)).removeTenant(apartmentSharing, cotenant);

        }

    }

    @Nested
    class LinkTenantToPartnerTest {

        @Test
        void shouldDoNothingIfPartnerDoesNotExist() {
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            when(userApiService.findByName("partner")).thenReturn(Optional.empty());

            userService.linkTenantToPartner(tenant, "partner", "internalPartnerId");

            verify(partnerCallBackService, times(0)).registerTenant(any(), any());
        }

        @Test
        void shouldLinkTenantToPartnerWhenNotCouple() {
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            var userApi = new UserApi();
            userApi.setId(1L);

            var appartementSharing = new ApartmentSharing();
            appartementSharing.setId(1L);
            appartementSharing.setApplicationType(ApplicationType.ALONE);
            appartementSharing.setTenants(List.of(tenant));

            tenant.setApartmentSharing(appartementSharing);

            when(userApiService.findByName("partner")).thenReturn(Optional.of(userApi));

            userService.linkTenantToPartner(tenant, "partner", "internalPartnerId");

            verify(partnerCallBackService, times(1)).registerTenant(tenant, userApi);
        }

        @Test
        void shouldLinkTenantToPartnerWhenCouple() {
            var tenant = new Tenant();
            tenant.setId(1L);
            tenant.setEmail("test@test.fr");

            var tenant2 = new Tenant();
            tenant2.setId(2L);
            tenant2.setEmail("test2@test.fr");

            var apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(1L);
            apartmentSharing.setTenants(List.of(tenant, tenant2));
            apartmentSharing.setApplicationType(ApplicationType.COUPLE);

            tenant.setApartmentSharing(apartmentSharing);
            tenant2.setApartmentSharing(apartmentSharing);

            var userApi = new UserApi();
            userApi.setId(1L);

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
            var tenant = new Tenant();
            tenant.setId(1L);

            when(userRepository.findById(tenant.getId())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    userService.unlinkFranceConnect(tenant));
        }

        @Test
        void shouldUnlinkFranceConnect() {
            var tenant = new Tenant();
            tenant.setId(1L);

            when (userRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

            userService.unlinkFranceConnect(tenant);

            verify(userRepository, times(1)).save(tenant);
            verify(keycloakService, times(1)).unlinkFranceConnect(tenant);

        }

    }
}
