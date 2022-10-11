package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.gouv.bo.dto.DocumentDeniedOptionsDTO;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentSubCategory.ALTERNATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DocumentDeniedOptionsServiceTest {

    private DocumentDeniedOptionsService service;
    private DocumentDeniedOptionsRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentDeniedOptionsRepository.class);
        service = new DocumentDeniedOptionsService(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void should_find_all_document_denied_options(String input) {
        service.findDocumentDeniedOptions(input);
        verify(repository).findAll();
    }

    @Test
    void should_find_documents_denied_options_by_category() {
        service.findDocumentDeniedOptions("ALTERNATION");
        verify(repository).findAllByDocumentSubCategory(ALTERNATION);
    }

    @Test
    void should_update_existing_message() {
        when(repository.findById(1)).thenReturn(Optional.of(new DocumentDeniedOptions()));

        service.updateMessage(1, "New message");

        assertThat(getSavedEntity().getMessageValue()).isEqualTo("New message");
    }

    @Test
    void should_create_new_document_denied_option() {
        DocumentDeniedOptionsDTO option = new DocumentDeniedOptionsDTO();
        option.setDocumentSubCategory(ALTERNATION);
        option.setDocumentUserType("tenant");
        option.setMessageValue("Message");

        service.createDocumentDeniedOption(option);

        DocumentDeniedOptions savedEntity = getSavedEntity();

        assertThat(savedEntity.getDocumentSubCategory()).isEqualTo(ALTERNATION);
        assertThat(savedEntity.getDocumentUserType()).isEqualTo("tenant");
        assertThat(savedEntity.getCode()).isEqualTo("T_ALTERNATION_001");
        assertThat(savedEntity.getMessageValue()).isEqualTo("Message");
    }

    private DocumentDeniedOptions getSavedEntity() {
        ArgumentCaptor<DocumentDeniedOptions> captor = ArgumentCaptor.forClass(DocumentDeniedOptions.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}