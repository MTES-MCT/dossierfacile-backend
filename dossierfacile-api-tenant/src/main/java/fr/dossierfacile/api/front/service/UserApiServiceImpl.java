package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.UserApiNotFoundException;
import fr.dossierfacile.api.front.repository.UserApiRepository;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserApiServiceImpl implements UserApiService {

    private final UserApiRepository userApiRepository;

    @Override
    public UserApi findById(Long id) {
        return userApiRepository.findById(id)
                .orElseThrow(() -> new UserApiNotFoundException(id));
    }
}
