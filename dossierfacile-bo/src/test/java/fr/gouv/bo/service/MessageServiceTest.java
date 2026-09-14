package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceTest {

    private MessageRepository messageRepository;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        messageService = new MessageService(messageRepository);
    }

    @Test
    void create_shouldCleanHtmlAndStripScriptTags() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        MessageDTO messageDTO = MessageDTO.builder()
                .message("<p>Hello</p><script>alert('XSS')</script><img src=x onerror=alert(1)>")
                .emailHtml("<b>Email</b><script>alert('mail')</script>")
                .build();

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message created = messageService.create(messageDTO, tenant, false, false);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();

        assertThat(saved.getMessageBody()).doesNotContain("<script>");
        assertThat(saved.getMessageBody()).doesNotContain("onerror");
        assertThat(saved.getMessageBody()).contains("<p>Hello</p>");

        assertThat(saved.getEmailHtml()).doesNotContain("<script>");
        assertThat(saved.getEmailHtml()).contains("<b>Email</b>");
    }

    @Test
    void findTenantMessages_shouldCleanHtmlOnRetrievedMessages() {
        Tenant user = new Tenant();
        user.setId(2L);

        Message dirtyMessage = Message.builder()
                .messageBody("Dangerous <script>alert('pwned')</script>content")
                .build();
        List<Message> list = new ArrayList<>(List.of(dirtyMessage));
        when(messageRepository.findByFromUserOrToUserOrderByCreationDateTimeDesc(user, user)).thenReturn(list);

        List<Message> results = messageService.findTenantMessages(user);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getMessageBody()).doesNotContain("<script>");
        assertThat(results.getFirst().getMessageBody()).isEqualTo("Dangerous content");
    }
}
