package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class DocumentVerifiedContent {

    private static final String UNKNOWN_ERROR_RESPONSE = "Cette URL n'est pas valide pour Mon FranceConnect";
    private static final String COMMA = ",";

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
    public boolean isMatchingWith(String actualFileContent) {
        List<String> expectedElements = elements.stream()
                .flatMap(element -> Arrays.stream(element.split(COMMA)))
                .collect(Collectors.toList());

        String contentToSearch = actualFileContent.replaceAll(COMMA, "");
        for (String expectedElement : expectedElements) {
            if (!contentToSearch.contains(expectedElement)) {
                return false;
            }
        }
        return true;
    }

    public boolean isDocumentUnknown() {
        return elements.get(0).contains(UNKNOWN_ERROR_RESPONSE);
    }

}
