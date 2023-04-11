package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.repository.UserApiRepository;
import fr.gouv.bo.utils.UtilsLocatio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserApiService {

    private final UserApiRepository userApiRepository;
    private final ObjectMapper mapper;

    public UserApi findById(Long id) {
        return userApiRepository.findOneById(id);
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
        return userApiRepository.findAll();
    }

    public UserApi create(UserApiDTO userApiDTO) {
        UserApi userApi = mapper.convertValue(userApiDTO, UserApi.class);
        if (StringUtils.isBlank(userApi.getApiKey())) {
            String apiKey = UtilsLocatio.generateRandomString(20);
            userApi.setApiKey(apiKey);
        }
        return userApiRepository.save(userApi);
    }

    public void save(UserApiDTO userApiDTO) {
        UserApi userApi = mapper.convertValue(userApiDTO, UserApi.class);
        userApiRepository.save(userApi);
    }

    public List<UserApi> findAllLightApi() {
        return userApiRepository.findAllByTypeUserApi(TypeUserApi.LIGHT);
    }

    public List<UserApi> getAllPartners() {
        return userApiRepository.findAll();
    }

}
