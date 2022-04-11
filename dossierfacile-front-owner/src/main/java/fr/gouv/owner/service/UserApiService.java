package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import fr.dossierfacile.common.service.interfaces.OvhService;
import fr.gouv.owner.dto.UserApiDTO;
import fr.gouv.owner.repository.UserApiRepository;
import fr.gouv.owner.utils.UtilsLocatio;
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
    private final OvhService ovhService;

    public UserApi findById(Long id) {
        return userApiRepository.findOneById(id);
    }

    public List<UserApi> findAll() {
        return userApiRepository.findAll();
    }

    public UserApi create(UserApiDTO userApiDTO) {
        UserApi userApi = new UserApi(userApiDTO.getName(), userApiDTO.getUrlCallback(), userApiDTO.getSite(), userApiDTO.getName2(), userApiDTO.getTextModal());
        String username = UtilsLocatio.generateRandomString(12);
        String apiKey = UtilsLocatio.generateRandomString(20);
        userApi.setApiKey(apiKey);
        userApi.setTypeUserApi(userApiDTO.getTypeUserApi());
        userApi.setTextModal(userApiDTO.getTextModal());
        userApi.setPartnerApiKeyCallback(userApiDTO.getPartnerApiKeyCallback());
        if (userApiDTO.getLogo() != null && !Objects.requireNonNull(userApiDTO.getLogo().getOriginalFilename()).isEmpty()) {
            try {
                ovhService.upload("logoApi/" + username + ".png", userApiDTO.getLogo().getInputStream());
                userApi.setLogo(true);
            } catch (IOException e) {
                log.error(e.getMessage(), e.getCause());
            }
        }
        return userApiRepository.save(userApi);
    }

    public UserApi findOrCreate(String source) {
        UserApi userApi = userApiRepository.findOneByName(source);
        if (userApi == null) {
            userApi = create(new UserApiDTO(source, TypeUserApi.LIGHT));
        }
        return userApi;
    }


    public UserApi findOneByName(String name) {
        return userApiRepository.findOneByName(name);
    }

    public List<UserApi> findAllLightApi() {
        return userApiRepository.findAllByTypeUserApi(TypeUserApi.LIGHT);
    }
}
