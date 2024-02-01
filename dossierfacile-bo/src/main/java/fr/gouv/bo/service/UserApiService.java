package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.repository.UserApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserApiService {
    private final KeycloakService keycloakService;
    private final UserApiRepository userApiRepository;
    private final ObjectMapper mapper;

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

}
