package fr.dossierfacile.process.file.service.monfranceconnect.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Getter
@AllArgsConstructor
public class DocumentVerifiedContent {

    public static final String COMMA = ",";
    private final List<String> elements;

    static Optional<DocumentVerifiedContent> from(ResponseEntity<String[]> response) {
        return Optional.ofNullable(response)
                .map(HttpEntity::getBody)
                .map(Arrays::asList)
                .filter(list -> !list.isEmpty())
                .map(DocumentVerifiedContent::new);
    }

    /**
     * The comparison between file content and MonFranceConnect API response is
     * ignoring commas, because some fields are not formatted the same way on both
     * sides (e.g. "Firstname, Lastname" instead of "Firstname Lastname").
     */
    public boolean isMatchingWithFile(Long fileId, String actualFileContent) {
        List<String> expectedElements = elements.stream()
                .flatMap(element -> Arrays.stream(element.split(COMMA)))
                .collect(Collectors.toList());
        List<String> missingElements = new ArrayList<>();

        String contentToSearch = actualFileContent.replaceAll(COMMA, "");
        for (String expectedElement : expectedElements) {
            if (!contentToSearch.contains(expectedElement)) {
                missingElements.add(expectedElement);
            }
        }

        if (missingElements.isEmpty()) {
            return true;
        }
        log.info("MFC document with ID {} is not matching with data from API (missing elements: {})", fileId, missingElements);
        return false;
    }

}
