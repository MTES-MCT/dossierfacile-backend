package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.UserApiRepository;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.common.entity.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {
    private final UserApiRepository userApiRepository;

    @Override
    public Optional<UserApi> findByName(String partner) {
        return userApiRepository.findByName(partner);
    }
}
