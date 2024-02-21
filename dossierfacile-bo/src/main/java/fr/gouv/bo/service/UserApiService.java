package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.OperationAccessToken;
import fr.dossierfacile.common.entity.TokenOperationAccessType;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.service.interfaces.OperationAccessTokenService;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.repository.UserApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserApiService {
    private final KeycloakService keycloakService;
    private final UserApiRepository userApiRepository;
    private final MailService mailService;
    private final OperationAccessTokenService operationAccessTokenService;
    private final ObjectMapper mapper;
    @Value("${display.client.secret.expiration.delay}")
    private Long displayClientSecretExpirationDelay;

    public UserApi findById(Long id) {
        return userApiRepository.findOneById(id).orElseThrow(NotFoundException::new);
    }

    public List<String> getNamesOfPartnerByTenantId(Long id) {
        return findPartnersLinkedToTenant(id).stream()
                .map(UserApi::getName)
                .collect(Collectors.toList());
    }

    public List<UserApi> findPartnersLinkedToTenant(Long id) {
        return userApiRepository.findPartnersLinkedToTenant(id);
    }

    public List<UserApi> findAll() {
        return userApiRepository.findAll(Sort.by("disabled", "name"));
    }

    public UserApi create(UserApiDTO userApiDTO) {
        UserApi userApi = mapper.convertValue(userApiDTO, UserApi.class);
        keycloakService.createKeycloakClient(userApi);
        return userApiRepository.save(userApi);
    }

    public void save(UserApiDTO userApiDTO) {
        UserApi userApi = mapper.convertValue(userApiDTO, UserApi.class);
        userApiRepository.save(userApi);
    }

    public List<UserApi> getAllPartners() {
        return userApiRepository.findAll();
    }

    public void sendMailWithConfig(String partnerEmail, UserApi userApi) {
        log.info("configuration for " + userApi.getName() + " is sending to " + partnerEmail);
        ClientRepresentation client = keycloakService.getKeyCloakClient(userApi.getName());
        OperationAccessToken token = OperationAccessToken.builder()
                .email(partnerEmail)
                .expirationDate(LocalDateTime.now().plusDays(displayClientSecretExpirationDelay))
                .operationAccessType(TokenOperationAccessType.DISPLAY_CLIENT_SECRET)
                .content(client.getSecret())
                .token(UUID.randomUUID().toString())
                .build();

        operationAccessTokenService.save(token);
        mailService.sendClientConfiguration(userApi, client, partnerEmail, token.getToken());
    }
}
