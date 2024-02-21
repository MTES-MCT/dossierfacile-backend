package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.common.entity.OperationAccessToken;
import fr.dossierfacile.common.service.interfaces.OperationAccessTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/onetimesecret")
@RequiredArgsConstructor
@Slf4j
public class OneTimeSecretController {
    private final OperationAccessTokenService operationAccessTokenService;

    @GetMapping(value = "/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> displayContent(@PathVariable String token) {
        OperationAccessToken operationAccessToken = operationAccessTokenService.findByToken(token);
        String content = operationAccessToken.getContent();
        operationAccessTokenService.delete(operationAccessToken);
        return ok(content);
    }
}
