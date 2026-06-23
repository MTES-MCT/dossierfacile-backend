package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.enums.AuthProvider;
import fr.dossierfacile.common.enums.UserType;
import fr.gouv.bo.repository.BOUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String OPERATOR_EMAIL = "e2e-tests@dossierfacile.fr";

    private BOUserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(BOUserRepository.class);
        userService = new UserService(
                userRepository,
                null, // userRoleRepository
                null, // tenantRepository
                null, // mailService
                null, // keycloakService
                null, // apartmentSharingRepository
                null, // propertyApartmentSharingRepository
                null, // apartmentSharingService
                null, // partnerCallBackService
                null, // tenantLogCommonService
                null  // tenantMapperForMail
        );
    }

    @Test
    void findOrCreateOperatorByEmail_returnsExistingOperatorWithoutCreatingIt() {
        BOUser existing = BOUser.builder().email(OPERATOR_EMAIL).build();
        when(userRepository.findByEmail(OPERATOR_EMAIL)).thenReturn(Optional.of(existing));

        BOUser operator = userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL);

        assertThat(operator).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateOperatorByEmail_createsDedicatedOperatorOnFirstUse() {
        when(userRepository.findByEmail(OPERATOR_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(BOUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BOUser operator = userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL);

        ArgumentCaptor<BOUser> captor = ArgumentCaptor.forClass(BOUser.class);
        verify(userRepository).save(captor.capture());
        BOUser created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo(OPERATOR_EMAIL);
        assertThat(created.getUserType()).isEqualTo(UserType.BO);
        assertThat(created.getProvider()).isEqualTo(AuthProvider.keycloak);
        assertThat(operator).isSameAs(created);
    }
}
