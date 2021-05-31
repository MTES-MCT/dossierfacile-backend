package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.UserApiRepository;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {
    private final UserApiRepository userApiRepository;

    @Override
    public UserApi findOrCreate(String name) {
        UserApi userApi = userApiRepository.findByName(name).orElse(
                UserApi.builder()
                        .apiKey(UUID.randomUUID().toString())
                        .name(name)
                        .typeUserApi(TypeUserApi.LIGHT)
                        .version(1)
                        .build()
        );
        return userApiRepository.save(userApi);
    }
}
