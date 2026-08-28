package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.gouv.bo.dto.CustomMessage;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.MessageItem;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.repository.GuarantorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceSendCustomMessageTest {

    @Mock
    private MessageSource messageSource;
    @Mock
    private MessageService messageService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentDeniedReasonsRepository documentDeniedReasonsRepository;
    @Mock
    private DocumentDeniedReasonsService documentDeniedReasonsService;
    @Mock
    private DocumentService documentService;
    @Mock
    private GuarantorRepository guarantorRepository;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                null, null, null, null,
                messageSource,
                documentRepository,
                documentDeniedReasonsRepository,
                messageService,
                null, null,
                documentDeniedReasonsService,
                documentService,
                null, null, null, null,
                guarantorRepository,
                null, null, null, null, null, null,
                null, // completedEligibilityService
                null,
                null
        );

        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(messageService.create(any(MessageDTO.class), any(Tenant.class), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> {
                    MessageDTO dto = invocation.getArgument(0);
                    return Message.builder().messageBody(dto.getMessage()).build();
                });
    }

    @Test
    void sendCustomMessage_shouldEscapeXssPayloadInFirstName() {
        String xssPayload = "<script>alert('XSS')</script>";
        Tenant tenant = Tenant.builder()
                .firstName(xssPayload)
                .lastName("Doe")
                .build();

        when(documentRepository.findById(1L)).thenReturn(java.util.Optional.of(new fr.dossierfacile.common.entity.Document()));

        MessageItem item = MessageItem.builder()
                .documentId(1L)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .commentDoc("Sample comment")
                .itemDetailList(Collections.emptyList())
                .build();

        CustomMessage customMessage = CustomMessage.builder()
                .messageItems(List.of(item))
                .guarantorItems(Collections.emptyList())
                .build();

        Message message = tenantService.sendCustomMessage(tenant, customMessage);

        assertThat(message).isNotNull();
        // The messageBody must NOT contain raw script tags from tenant's firstName
        assertThat(message.getMessageBody()).doesNotContain("<script>alert('XSS')</script>");
        // The messageBody should contain HTML-escaped content
        assertThat(message.getMessageBody()).contains("&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;");
    }
}
