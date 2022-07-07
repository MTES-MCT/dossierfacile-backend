package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.repository.UserApiRepository;
import fr.gouv.bo.utils.UtilsLocatio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserApiService {

    private final UserApiRepository userApiRepository;
    private final FileStorageService fileStorageService;

    public UserApi findById(Long id) {
        return userApiRepository.findOneById(id);
    }

    public List<String> getNamesOfPartnerByTenantId(Long id) {
        return userApiRepository.getNamesOfPartnerByTenantId(id);
    }

    public List<UserApi> findAll() {
        return userApiRepository.findAll();
    }

    public UserApi create(UserApiDTO userApiDTO) {
        UserApi userApi = UserApi.builder()
                .name(userApiDTO.getName())
                .name2(userApiDTO.getName2())
                .urlCallback(userApiDTO.getUrlCallback())
                .site(userApiDTO.getSite())
                .textModal(userApiDTO.getTextModal())
                .build();
        String apiKey = UtilsLocatio.generateRandomString(20);
        userApi.setApiKey(apiKey);
        userApi.setTypeUserApi(userApiDTO.getTypeUserApi());
        userApi.setTextModal(userApiDTO.getTextModal());
        userApi.setPartnerApiKeyCallback(userApiDTO.getPartnerApiKeyCallback());
        if (userApiDTO.getLogo() != null && !Objects.requireNonNull(userApiDTO.getLogo().getOriginalFilename()).isEmpty()) {
            try {
                fileStorageService.upload("logoApi/" + UtilsLocatio.generateRandomString(12) + ".png", userApiDTO.getLogo().getInputStream(), null);
                userApi.setLogo(true);
            } catch (IOException e) {
                log.error(e.getMessage(), e.getCause());
            }
        }
        return userApiRepository.save(userApi);
    }

    public void save(UserApiDTO userApiDTO) {
        UserApi userApi = findById(userApiDTO.getId());
        userApi.setUrlCallback(userApiDTO.getUrlCallback());
        userApi.setName2(userApiDTO.getName2());
        userApi.setSite(userApiDTO.getSite());
        userApi.setTextModal(userApiDTO.getTextModal());
        userApi.setPartnerApiKeyCallback(userApiDTO.getPartnerApiKeyCallback());
        if (!Objects.requireNonNull(userApiDTO.getLogo().getOriginalFilename()).isEmpty()) {
            try {
                fileStorageService.upload("logoApi/" + UtilsLocatio.generateRandomString(12) + ".png", userApiDTO.getLogo().getInputStream(), null);
                userApi.setLogo(true);
            } catch (IOException e) {
                log.error(e.getMessage(), e.getCause());
            }
        }
        userApiRepository.save(userApi);
    }

    public UserApi findOneByName(String name) {
        return userApiRepository.findOneByName(name);
    }

    public List<UserApi> findAllLightApi() {
        return userApiRepository.findAllByTypeUserApi(TypeUserApi.LIGHT);
    }

    public List<UserApi> getAllPartners() {
        return userApiRepository.findAll();
    }

}
