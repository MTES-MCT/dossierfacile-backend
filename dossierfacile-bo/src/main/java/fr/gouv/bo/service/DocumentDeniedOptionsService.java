package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.gouv.bo.dto.DocumentDeniedOptionsDTO;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@AllArgsConstructor
public class DocumentDeniedOptionsService {

    private final DocumentDeniedOptionsRepository repository;

    public Optional<DocumentDeniedOptions> findDocumentDeniedOption(int id) {
        return repository.findById(id);
    }

    public List<DocumentDeniedOptions> findDocumentDeniedOptions(String documentCategory, String documentSubCategory) {
        return repository.findAllByDocumentSubCategoryAndDocumentCategory(documentCategory, documentSubCategory);
    }

    public void updateMessage(int id, String message) {
        repository.findById(id).ifPresent(option -> {
            option.setMessageValue(message);
            repository.save(option);
        });
    }

    public void createDocumentDeniedOption(DocumentDeniedOptionsDTO createdOption) {
        DocumentSubCategory subCategory = createdOption.getDocumentSubCategory();
        DocumentCategory category = createdOption.getDocumentCategory();
        String userType = createdOption.getDocumentUserType();
        DocumentDeniedOptions documentDeniedOptions = DocumentDeniedOptions.builder()
                .documentCategory(category)
                .documentSubCategory(subCategory)
                .documentUserType(userType)
                .messageValue(createdOption.getMessageValue())
                .code(buildNewCode(subCategory, userType))
                .build();
        repository.save(documentDeniedOptions);
    }

    public void deleteDocumentDeniedOption(int id) {
        repository.deleteById(id);
    }

    private String buildNewCode(DocumentSubCategory category, String userType) {
        int lastExisting = repository.findAllByDocumentSubCategoryAndDocumentUserTypeOrderByCode(category, userType)
                .stream()
                .map(DocumentDeniedOptions::getCode)
                .mapToInt(code -> Integer.parseInt(code.substring(code.length() - 3)))
                .max()
                .orElse(0);
        return String.format("%s_%s_%03d", userType.substring(0, 1).toUpperCase(), category, lastExisting + 1);
    }
}
