package fr.dossierfacile.process.file.service.monfranceconnect.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DocumentVerifiedContentTest {

    private static final String FILE_CONTENT = "John, Doe 01/01/19801000 €";

    @Test
    void should_build_object_from_api_response() {
        var apiResponse = ResponseEntity.ok(new String[]{"a", "b"});
        var verifiedContent = DocumentVerifiedContent.from(apiResponse);
        assertThat(verifiedContent).isPresent()
                .contains(new DocumentVerifiedContent(List.of("a", "b")));
    }

    @Test
    void should_return_empty_if_api_response_is_empty() {
        var apiResponse = ResponseEntity.ok(new String[]{});
        var verifiedContent = DocumentVerifiedContent.from(apiResponse);
        assertThat(verifiedContent).isEmpty();
    }

    @ParameterizedTest
    @MethodSource
    void should_match_with_actual_file_content(List<String> elements) {
        var verifiedContent = new DocumentVerifiedContent(elements);
        assertThat(verifiedContent.isMatchingWith(FILE_CONTENT)).isTrue();
    }

    public static Stream<Arguments> should_match_with_actual_file_content() {
        return Stream.of(
                arguments(List.of("John Doe", "01/01/1980", "1000 €")),
                arguments(List.of("John Doe", "01/01/1980")),
                arguments(List.of("1000 €", "01/01/1980"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void should_not_match_with_actual_file_content(List<String> elements) {
        var verifiedContent = new DocumentVerifiedContent(elements);
        assertThat(verifiedContent.isMatchingWith(FILE_CONTENT)).isFalse();
    }

    public static Stream<Arguments> should_not_match_with_actual_file_content() {
        return Stream.of(
                arguments(List.of("John Doe", "01/01/1981", "1000 €")),
                arguments(List.of("John Doe", "01/01/1980", "1001 €")),
                arguments(List.of("1001 €"))
        );
    }

}