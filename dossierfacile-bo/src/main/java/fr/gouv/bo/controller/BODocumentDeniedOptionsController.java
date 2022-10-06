package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.gouv.bo.dto.DocumentDeniedOptionsDTO;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
@RequestMapping("/bo/documentDeniedOptions")
@AllArgsConstructor
public class BODocumentDeniedOptionsController {

    private static final String EMAIL = "email";

    private final DocumentDeniedOptionsRepository repository;

    @GetMapping
    public String documentDeniedOptions(Model model,
                                        @RequestParam(value = "documentSubCategory", required = false) String documentSubCategory) {
        List<DocumentDeniedOptions> documentDeniedOptions = findDocumentDeniedOptions(documentSubCategory);
        documentDeniedOptions.sort(comparing(DocumentDeniedOptions::getCode));

        model.addAttribute("documentDeniedOptions", documentDeniedOptions);
        model.addAttribute("documentSubCategories", DocumentSubCategory.values());
        model.addAttribute(EMAIL, new EmailDTO());

        return "bo/document-denied-options";
    }

    @GetMapping("/{code}")
    public String editDocumentDeniedOption(Model model,
                                           @PathVariable(value = "code") String optionCode) {
        DocumentDeniedOptions optionToEdit = repository.findByCode(optionCode);
        model.addAttribute("documentDeniedOption", optionToEdit);
        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/edit-document-denied-option";
    }

    @PostMapping("/{code}")
    public String saveDocumentDeniedOption(@PathVariable(value = "code") String optionCode,
                                           @ModelAttribute("documentDeniedOption") DocumentDeniedOptionsDTO modifiedOption) {
        String newMessage = modifiedOption.getMessageValue();
        DocumentDeniedOptions documentDeniedOptions = repository.findByCode(optionCode);
        documentDeniedOptions.setMessageValue(newMessage);
        repository.save(documentDeniedOptions);
        return "redirect:/bo/documentDeniedOptions";
    }

    private List<DocumentDeniedOptions> findDocumentDeniedOptions(String documentSubCategory) {
        if (isNotBlank(documentSubCategory)) {
            return repository.findAllByDocumentSubCategory(DocumentSubCategory.valueOf(documentSubCategory));
        }
        return repository.findAll();
    }

}
