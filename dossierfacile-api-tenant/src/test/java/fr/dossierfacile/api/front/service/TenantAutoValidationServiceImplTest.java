package fr.dossierfacile.api.front.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantAutoValidationServiceImplTest {

    private TenantAutoValidationServiceImpl tenantAutoValidationService;

    @BeforeEach
    void setUp() {
        tenantAutoValidationService = new TenantAutoValidationServiceImpl();
    }

    @Test
    @DisplayName("Should return true when document subCategory is VISALE")
    void isEligibleForAutoValidation_visale_returnsTrue() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.VISALE)
                .build();

        boolean eligible = tenantAutoValidationService.isEligibleForAutoValidation(document);

        assertTrue(eligible);
    }

    @Test
    @DisplayName("Should return false when document subCategory is not VISALE")
    void isEligibleForAutoValidation_otherSubCategory_returnsFalse() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.SALARY)
                .build();

        boolean eligible = tenantAutoValidationService.isEligibleForAutoValidation(document);

        assertFalse(eligible);
    }

    @Test
    @DisplayName("Should return false when document or subCategory is null")
    void isEligibleForAutoValidation_nullDocument_returnsFalse() {
        assertFalse(tenantAutoValidationService.isEligibleForAutoValidation(null));
        assertFalse(tenantAutoValidationService.isEligibleForAutoValidation(Document.builder().build()));
    }
}
