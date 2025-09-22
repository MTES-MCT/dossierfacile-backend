package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.gouv.bo.dto.DocumentDeniedOptionsDTO;
import fr.gouv.bo.service.DocumentDeniedOptionsService;
import fr.gouv.bo.utils.DateFormatUtil;
import fr.gouv.bo.utils.DocumentLabelUtils;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

@Controller
@AllArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@RequestMapping("/bo/documentDeniedOptions")
public class BODocumentDeniedOptionsController {

    private static final String MONTH_N = "monthN";
    private static final List<String> DOCUMENT_USER_TYPES = List.of("tenant", "guarantor");

    private final DocumentDeniedOptionsService service;

    @GetMapping
    public String documentDeniedOptions(
            Model model,
            @RequestParam(value = "documentSubCategory", required = false) String documentSubCategory,
            @RequestParam(value = "documentCategory", required = false) String documentCategory
    ) {
        List<DocumentDeniedOptions> documentDeniedOptions = service.findDocumentDeniedOptions(documentCategory, documentSubCategory);
        documentDeniedOptions.sort(DocumentDeniedOptions::compareDocumentDeniedOptions);
        model.addAttribute("documentDeniedOptions", documentDeniedOptions);
        model.addAttribute("documentUndefinedSubCategory", DocumentSubCategory.UNDEFINED);
        model.addAttribute("documentSubCategories", DocumentSubCategory.alphabeticallySortedValues().stream().filter(item -> item != DocumentSubCategory.UNDEFINED));
        model.addAttribute("documentUndefinedCategory", DocumentCategory.NULL);
        model.addAttribute("documentCategories", DocumentCategory.alphabeticallySortedValues().stream().filter(item -> item != DocumentCategory.NULL));
        model.addAttribute("documentLabelUtils", new DocumentLabelUtils());

        return "bo/document-denied-options";
    }

    @GetMapping("/{id}")
    public String editDocumentDeniedOption(Model model, @PathVariable(value = "id") int id) {
        Optional<DocumentDeniedOptions> optionToEdit = service.findDocumentDeniedOption(id);
        if (optionToEdit.isEmpty()) {
            return "redirect:/bo/documentDeniedOptions";
        }
        model.addAttribute("documentDeniedOption", optionToEdit.get());
        model.addAttribute("documentLabelUtils", new DocumentLabelUtils());
        model.addAttribute(MONTH_N, DateFormatUtil.replaceMonthPlaceholder("{mois}"));
        for (int i = 1; i <= 6; i++) {
            model.addAttribute(MONTH_N + i, DateFormatUtil.replaceMonthPlaceholder(String.format("{moisN-%d}", i)));
        }
        return "bo/edit-document-denied-option";
    }

    @GetMapping("/create")
    public String createDocumentDeniedOption(Model model) {
        model.addAttribute("documentUndefinedCategory", DocumentSubCategory.UNDEFINED);
        model.addAttribute("documentNullCategory", DocumentCategory.NULL);
        model.addAttribute("documentCategories", DocumentCategory.alphabeticallySortedValues().stream().filter(item -> item != DocumentCategory.NULL));
        model.addAttribute("documentSubCategories", DocumentSubCategory.alphabeticallySortedValues().stream().filter(item -> item != DocumentSubCategory.UNDEFINED));
        model.addAttribute("documentUserTypes", DOCUMENT_USER_TYPES);
        model.addAttribute("documentDeniedOption", new DocumentDeniedOptionsDTO());
        model.addAttribute("documentLabelUtils", new DocumentLabelUtils());
        model.addAttribute(MONTH_N, DateFormatUtil.replaceMonthPlaceholder("{mois}"));
        for (int i = 1; i <= 6; i++) {
            model.addAttribute(MONTH_N + i, DateFormatUtil.replaceMonthPlaceholder(String.format("{moisN-%d}", i)));
        }
        return "bo/create-document-denied-option";
    }

    @PostMapping
    public String saveNewDocumentDeniedOption(@ModelAttribute("documentDeniedOption") DocumentDeniedOptionsDTO createdOption) {
        service.createDocumentDeniedOption(createdOption);
        return "redirect:/bo/documentDeniedOptions";
    }

    @PostMapping("/{id}")
    public String saveDocumentDeniedOption(@PathVariable(value = "id") int id,
                                           @ModelAttribute("documentDeniedOption") DocumentDeniedOptionsDTO modifiedOption) {
        service.updateMessage(id, modifiedOption.getMessageValue());
        return "redirect:/bo/documentDeniedOptions";
    }

    @DeleteMapping("/{id}")
    public String deleteDocumentDeniedOption(@PathVariable(value = "id") int id) {
        service.deleteDocumentDeniedOption(id);
        return "redirect:/bo/documentDeniedOptions";
    }

}
