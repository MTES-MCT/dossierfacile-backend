package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.AuthenticationFacadeImpl;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.TenantServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.tenant.tax.TenantNumberOfDocumentTaxValidator;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NumberOfDocumentTaxValidatorTest {
    private final AuthenticationFacade authenticationFacade = mock(AuthenticationFacadeImpl.class);
    private final TenantService tenantService = mock(TenantServiceImpl.class);
    private final FileRepository fileRepository = mock(FileRepository.class);

    private final TenantNumberOfDocumentTaxValidator validator = new TenantNumberOfDocumentTaxValidator(fileRepository);


    private Tenant tenant = Tenant.builder().id(1L).firstName("John").lastName("Doe").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "authenticationFacade", authenticationFacade);
        ReflectionTestUtils.setField(validator, "tenantService", tenantService);
    }

    @Test
    void whenMyTaxWithGoodNumberOfDocuments() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(1L);
        var documentTaxForm = new DocumentTaxForm();
        var mockFile = new MockMultipartFile("test", "test".getBytes());

        documentTaxForm.setDocuments(List.of(mockFile));
        documentTaxForm.setNoDocument(false);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        assertThat(validator.isValid(documentTaxForm, null)).isTrue();
    }

    @Test
    void whenMyTaxWithTooManyDocuments() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(4L);

        var validationContext = getMockedValidationContext();

        var documentTaxForm = new DocumentTaxForm();
        var mockFile = new MockMultipartFile("test", "test".getBytes());

        documentTaxForm.setDocuments(List.of(mockFile, mockFile));
        documentTaxForm.setNoDocument(false);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        assertThat(validator.isValid(documentTaxForm, validationContext)).isFalse();

        verify(validationContext, times(1)).buildConstraintViolationWithTemplate(TenantNumberOfDocumentTaxValidator.TOO_MANY_DOCUMENTS_RESPONSE);
    }

    @Test
    void whenMyTaxNoDocument() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(0L);

        var validationContext = getMockedValidationContext();

        var documentTaxForm = new DocumentTaxForm();

        documentTaxForm.setDocuments(List.of());
        documentTaxForm.setNoDocument(false);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        assertThat(validator.isValid(documentTaxForm, validationContext)).isFalse();

        verify(validationContext, times(1)).buildConstraintViolationWithTemplate(TenantNumberOfDocumentTaxValidator.MISSING_DOCUMENT_RESPONSE);
    }

    @Test
    void whenMyParentTaxWithNoDocuments() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(0L);

        var documentTaxForm = new DocumentTaxForm();

        documentTaxForm.setDocuments(List.of());
        documentTaxForm.setNoDocument(false);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.MY_PARENTS);
        assertThat(validator.isValid(documentTaxForm, null)).isTrue();
    }

    @Test
    void whenMyParentTaxWithDocuments() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(0L);

        var validationContext = getMockedValidationContext();

        var mockFile = new MockMultipartFile("test", "test".getBytes());
        var documentTaxForm = new DocumentTaxForm();

        documentTaxForm.setDocuments(List.of(mockFile));
        documentTaxForm.setNoDocument(false);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.MY_PARENTS);
        assertThat(validator.isValid(documentTaxForm, validationContext)).isFalse();

        verify(validationContext, times(1)).buildConstraintViolationWithTemplate(TenantNumberOfDocumentTaxValidator.NO_DOCUMENT_RESPONSE);
    }

    @Test
    void whenOtherTaxWithDocument() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(0L);

        var validationContext = getMockedValidationContext();

        var mockFile = new MockMultipartFile("test", "test".getBytes());
        var documentTaxForm = new DocumentTaxForm();

        documentTaxForm.setDocuments(List.of(mockFile));
        documentTaxForm.setNoDocument(true);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.OTHER_TAX);

        assertThat(validator.isValid(documentTaxForm, validationContext)).isFalse();

        verify(validationContext, times(1)).buildConstraintViolationWithTemplate(TenantNumberOfDocumentTaxValidator.NO_DOCUMENT_RESPONSE);
    }

    @Test
    void whenOterTaxWithNoDocument() {
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenant);
        when(fileRepository.countFileByDocumentCategoryTenant(any(), any(Tenant.class)))
                .thenReturn(0L);

        var documentTaxForm = new DocumentTaxForm();

        documentTaxForm.setDocuments(List.of());
        documentTaxForm.setNoDocument(true);
        documentTaxForm.setAvisDetected(false);
        documentTaxForm.setTypeDocumentTax(DocumentSubCategory.OTHER_TAX);

        assertThat(validator.isValid(documentTaxForm, null)).isTrue();
    }


    private ConstraintValidatorContext getMockedValidationContext() {
        var validationContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder mockBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(mockBuilder.addPropertyNode(any())).thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class));
        when(validationContext.buildConstraintViolationWithTemplate(any())).thenReturn(mockBuilder);

        return validationContext;
    }
}


