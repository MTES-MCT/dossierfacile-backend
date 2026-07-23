package fr.dossierfacile.api.front.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

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

    @Nested
    @DisplayName("isTenantReadyForAutoValidation tests")
    class IsTenantReadyForAutoValidationTests {

        @Test
        @DisplayName("Should return true when all non-auto-validatable docs are VALIDATED and only VISALE is TO_PROCESS")
        void returnsTrue_whenOnlyVisaleIsToProcessAndOtherDocsAreValidated() {
            Document validatedDoc = Document.builder()
                    .documentStatus(DocumentStatus.VALIDATED)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(validatedDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertTrue(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when a non-auto-validatable doc (e.g. SALARY) is also TO_PROCESS")
        void returnsFalse_whenNonAutoValidatableDocIsToProcess() {
            Document salaryDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(salaryDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when a document is DECLINED")
        void returnsFalse_whenDocIsDeclined() {
            Document declinedDoc = Document.builder()
                    .documentStatus(DocumentStatus.DECLINED)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(declinedDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when no document is TO_PROCESS (all VALIDATED)")
        void returnsFalse_whenNoDocIsToProcess() {
            Document validatedDoc = Document.builder()
                    .documentStatus(DocumentStatus.VALIDATED)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(validatedDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }
    }
}
