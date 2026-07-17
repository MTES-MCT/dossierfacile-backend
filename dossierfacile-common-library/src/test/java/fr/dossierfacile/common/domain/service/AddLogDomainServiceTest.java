package fr.dossierfacile.common.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.repository.OperatorLogCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddLogDomainServiceTest {

    private AddLogDomainService service;

    @Mock
    private TenantLogRepository repository;

    @Mock
    private OperatorLogCommonRepository operatorLogRepository;

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<TenantLog> tenantLogCaptor;

    @BeforeEach
    void setUp() {
        service = new AddLogDomainService(repository, objectMapper, operatorLogRepository, tenantCommonRepository);
    }

    @Test
    void should_add_document_deleted_log() {
        // Given
        Document document = new Document(DocumentEntity.builder()
                .id(10L)
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .tenantId(2L)
                .build());
        Tenant editor = new Tenant(TenantEntity.builder().id(2L).build());
        Operator operator = new Operator(OperatorEntity.builder().id(99L).build());

        // When
        service.addDocumentDeletedLog(document, editor, Optional.of(operator));

        // Then
        verify(repository).save(tenantLogCaptor.capture());
        TenantLog logged = tenantLogCaptor.getValue();

        assertThat(logged.getLogType()).isEqualTo(LogType.DOCUMENT_DELETED);
        assertThat(logged.getTenantId()).isEqualTo(2L);
        assertThat(logged.getOperatorId()).isEqualTo(99L);
        assertThat(logged.getLogDetails().get("documentId").asLong()).isEqualTo(10L);
    }

    @Test
    void should_add_file_deleted_log() {
        // Given
        FileEntity file = FileEntity.builder()
                .id(100L)
                .build();
        Tenant editor = new Tenant(TenantEntity.builder().id(2L).build());

        // When
        service.addFileDeletedLog(file, editor, Optional.empty());

        // Then
        verify(repository).save(tenantLogCaptor.capture());
        TenantLog logged = tenantLogCaptor.getValue();

        assertThat(logged.getLogType()).isEqualTo(LogType.FILE_DELETED);
        assertThat(logged.getTenantId()).isEqualTo(2L);
        assertThat(logged.getOperatorId()).isNull();
    }
    
    @Test
    void should_add_account_validated_log() {
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).status(fr.dossierfacile.common.enums.TenantFileStatus.VALIDATED).build());
        fr.dossierfacile.common.entity.User operator = org.mockito.Mockito.mock(fr.dossierfacile.common.entity.User.class);
        org.mockito.Mockito.when(operator.getId()).thenReturn(99L);
        fr.dossierfacile.common.entity.Tenant legacyTenant = fr.dossierfacile.common.entity.Tenant.builder().id(1L).build();
        
        org.mockito.Mockito.when(tenantCommonRepository.getReferenceById(1L)).thenReturn(legacyTenant);
        
        service.addAccountValidatedLog(tenant, java.util.Optional.of(operator));
        
        verify(repository).save(org.mockito.ArgumentMatchers.any(TenantLog.class));
        verify(operatorLogRepository).save(org.mockito.ArgumentMatchers.any(fr.dossierfacile.common.entity.OperatorLog.class));
    }
    
    @Test
    void should_add_account_denied_log() {
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).status(fr.dossierfacile.common.enums.TenantFileStatus.DECLINED).build());
        fr.dossierfacile.common.entity.User operator = org.mockito.Mockito.mock(fr.dossierfacile.common.entity.User.class);
        org.mockito.Mockito.when(operator.getId()).thenReturn(99L);
        fr.dossierfacile.common.entity.Tenant legacyTenant = fr.dossierfacile.common.entity.Tenant.builder().id(1L).build();
        
        org.mockito.Mockito.when(tenantCommonRepository.getReferenceById(1L)).thenReturn(legacyTenant);
        
        service.addAccountDeniedLog(tenant, java.util.Optional.of(operator));
        
        verify(repository).save(org.mockito.ArgumentMatchers.any(TenantLog.class));
        verify(operatorLogRepository).save(org.mockito.ArgumentMatchers.any(fr.dossierfacile.common.entity.OperatorLog.class));
    }
}
