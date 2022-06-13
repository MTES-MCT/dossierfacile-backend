package fr.dossierfacile.process.file.service.interfaces;

import java.util.List;
import org.springframework.http.ResponseEntity;

public interface ApiMonFranceConnect {
    ResponseEntity<List> monFranceConnect(String url);
}
