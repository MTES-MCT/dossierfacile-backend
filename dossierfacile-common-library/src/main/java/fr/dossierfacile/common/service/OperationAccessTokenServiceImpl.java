package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.OperationAccessToken;
import fr.dossierfacile.common.repository.OperationAccessTokenRepository;
import fr.dossierfacile.common.service.interfaces.OperationAccessTokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class OperationAccessTokenServiceImpl implements OperationAccessTokenService {
    private OperationAccessTokenRepository operationAccessTokenRepository;

    @Override
    public List<OperationAccessToken> findExpiredToken() {
        return operationAccessTokenRepository.findAllByExpirationDateBefore(LocalDateTime.now());
    }

    @Override
    public void delete(OperationAccessToken token) {
        operationAccessTokenRepository.delete(token);
    }

    @Override
    public OperationAccessToken save(OperationAccessToken token) {
        return operationAccessTokenRepository.save(token);
    }

    @Override
    public OperationAccessToken findByToken(String token) {
        return operationAccessTokenRepository.findByToken(token);
    }
}