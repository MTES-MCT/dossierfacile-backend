package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantAutoValidationService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractDocumentSaveStepTest {

    @InjectMocks
    private TestDocumentSaveStep testDocumentSaveStep;

    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private DocumentService documentService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private TenantCommonRepository tenantCommonRepository;
    @Mock
    private LogService logService;
    @Mock
    private Producer producer;
    @Mock
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @Mock
    private FileUploadPreprocessor fileUploadPreprocessor;
    @Mock
    private TenantAutoValidationService tenantAutoValidationService;

    private static class TestDocumentForm extends DocumentForm {}

    private static class TestDocumentSaveStep extends AbstractDocumentSaveStep<TestDocumentForm> {
        public Document documentToReturn;

        @Override
        protected DocumentSaveResult saveDocument(Tenant tenant, TestDocumentForm documentForm) {
            return new DocumentSaveResult(documentToReturn, false);
        }
    }

    @BeforeEach
    void setUp() {
        when(tenantCommonRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantMapper.toTenantModel(any(), any())).thenReturn(TenantModel.builder().build());
    }

    @Nested
    @DisplayName("Tests for readyForAutoValidation flag delegation")
    class ReadyForAutoValidationTests {

        @Test
        @DisplayName("Should set readyForAutoValidation according to tenantAutoValidationService.isTenantReadyForAutoValidation result")
        void saveStep_setsFlagFromServiceResult() {
            Tenant tenant = Tenant.builder().id(100L).build();
            Document document = Document.builder().id(1L).tenant(tenant).documentSubCategory(DocumentSubCategory.VISALE).build();
            testDocumentSaveStep.documentToReturn = document;

            when(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant)).thenReturn(true);

            testDocumentSaveStep.saveStep(tenant, new TestDocumentForm());

            ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
            verify(tenantCommonRepository).save(tenantCaptor.capture());
            assertThat(tenantCaptor.getValue().getReadyForAutoValidation()).isTrue();
        }
    }
}
