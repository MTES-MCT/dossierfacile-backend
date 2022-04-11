package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.MessageStatus;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public List<Message> findTenantMessages(User tenant) {
        return messageRepository.findByFromUserOrToUserOrderByCreationDateTimeDesc(tenant, tenant);
    }

    public Message findLastMessageFromUser(Tenant tenant) {
        return messageRepository.findFirstByFromUserAndCustomMessageOrderByCreationDateTimeDesc(tenant, false);
    }

    public Message findLastMessageToUser(Tenant tenant) {
        return messageRepository.findFirstByToUserAndCustomMessageOrderByCreationDateTimeDesc(tenant, false);
    }

    public Message create(MessageDTO messageDTO, Tenant tenant, boolean isUser, boolean isCustomsMessage) {
        Message message = Message.builder()
                .messageBody(messageDTO.getMessage())
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

    public void createMistakeMessage(Tenant tenant) {
        String m = "Bonjour," +
                "<br/>" +
                "Au temps pour nous! Merci pour vos précisions, je valide tout de suite votre dossier." +
                "<br/>" +
                "En vous souhaitant une très bonne journée,<br/>" +
                "Marie pour l’équipe DossierFacile";
        Message message = Message.builder().messageBody(m).toUser(tenant).creationDateTime(LocalDateTime.now()).messageStatus(MessageStatus.UNREAD).build();
        messageRepository.save(message);
    }
}
