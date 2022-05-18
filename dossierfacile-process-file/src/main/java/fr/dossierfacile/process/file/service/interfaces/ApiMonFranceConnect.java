package fr.dossierfacile.process.file.service.interfaces;

import org.springframework.http.ResponseEntity;

public interface ApiMonFranceConnect {
    ResponseEntity<String> monFranceConnect(String url);
}
