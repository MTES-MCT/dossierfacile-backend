package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.UserApiNotFoundException;
import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.UserApiRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserApiServiceImpl implements UserApiService {

    private final UserApiRepository userApiRepository;

    @Override
    public UserApi findById(Long id) {
        return userApiRepository.findById(id)
                .orElseThrow(() -> new UserApiNotFoundException(id));
    }

    @Override
    public Optional<UserApi> findByName(String partner) {
        return userApiRepository.findByName(partner);
    }

    @Transactional
    @Override
    public UserApi update(UserApi userApiParam, PartnerSettings settings) {
        UserApi userApi = userApiRepository.findById(userApiParam.getId()).get();

        if (StringUtils.isNotBlank(settings.getEmail())) {
            // Email cannot be vacuumed - force to provide a new one
            userApi.setEmail(settings.getEmail());
        }
        if (settings.getUrlCallback() != null) {
            userApi.setUrlCallback(settings.getUrlCallback());
        }
        if (settings.getPartnerApiKeyCallback() != null) {
            userApi.setPartnerApiKeyCallback(settings.getPartnerApiKeyCallback());
        }
        userApiRepository.save(userApi);

        return userApi;
    }
}
