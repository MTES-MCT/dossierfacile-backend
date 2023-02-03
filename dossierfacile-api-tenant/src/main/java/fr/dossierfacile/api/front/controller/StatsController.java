package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.service.interfaces.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService statsService;

    @GetMapping(value = "/dossiers/validated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validatedCount() {
        return ok(statsService.getValidatedDossierCount());
    }
}
