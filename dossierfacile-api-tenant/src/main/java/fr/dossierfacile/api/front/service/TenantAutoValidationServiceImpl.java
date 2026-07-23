package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.TenantAutoValidationService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Service;

@Service
public class TenantAutoValidationServiceImpl implements TenantAutoValidationService {

    @Override
    public boolean isEligibleForAutoValidation(Document document) {
        if (document == null || document.getDocumentSubCategory() == null) {
            return false;
        }
        return document.getDocumentSubCategory() == DocumentSubCategory.VISALE;
    }

}
