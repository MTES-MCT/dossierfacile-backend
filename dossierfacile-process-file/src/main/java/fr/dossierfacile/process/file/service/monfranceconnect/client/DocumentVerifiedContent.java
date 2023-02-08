package fr.dossierfacile.process.file.service.monfranceconnect.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class DocumentVerifiedContent {

    private final List<String> elements;

    static Optional<DocumentVerifiedContent> from(ResponseEntity<String[]> response) {
        return Optional.ofNullable(response)
                .map(HttpEntity::getBody)
                .map(Arrays::asList)
                .map(DocumentVerifiedContent::new);
    }

}
