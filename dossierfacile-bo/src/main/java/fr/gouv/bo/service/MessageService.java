package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.MessageStatus;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dossierfacile.common.config.xss.XssSanitizer;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public List<Message> findTenantMessages(User tenant) {
        List<Message> messages = messageRepository.findByFromUserOrToUserOrderByCreationDateTimeDesc(tenant, tenant);
        messages.forEach(message -> message.setMessageBody(XssSanitizer.cleanHtml(message.getMessageBody())));
        return messages;
    }

    public Message create(MessageDTO messageDTO, Tenant tenant, boolean isUser, boolean isCustomsMessage) {
        String cleanMessage = XssSanitizer.cleanHtml(messageDTO.getMessage());
        String cleanEmailHtml = XssSanitizer.cleanHtml(messageDTO.getEmailHtml());
        Message message = Message.builder()
                .messageBody(cleanMessage)
                .emailHtml(cleanEmailHtml)
                .customMessage(isCustomsMessage)
                .messageStatus(MessageStatus.UNREAD)
                .creationDateTime(LocalDateTime.now())
                .build();

        if (isUser) {
            message.setFromUser(tenant);
        } else {
            message.setToUser(tenant);
        }
        return messageRepository.save(message);
    }

    @Transactional
    public void markReadAdmin(User tenant) {
        messageRepository.markReadAdmin(tenant);
    }

}
