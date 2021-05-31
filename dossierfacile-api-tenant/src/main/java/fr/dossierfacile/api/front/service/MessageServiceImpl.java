package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.form.MessageForm;
import fr.dossierfacile.api.front.mapper.MessageMapper;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.MessageRepository;
import fr.dossierfacile.api.front.service.interfaces.MessageService;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final DocumentRepository documentRepository;

    @Override
    public List<MessageModel> findAll(Tenant tenant) {
        return messageMapper.toListMessageModel(messageRepository.findByFromUserOrToUserOrderByCreationDateTimeDesc(tenant, tenant));
    }

    @Override
    public MessageModel create(Tenant tenant, MessageForm messageForm, boolean isCustomMessage) {

        String messageBody = messageForm.getMessageBody();

        //todo Remove this line when integrated the solution of BO for avoiding xss and html injections. (2021-05-01 11:56am)
        messageBody = messageBody.replace("<","&lt;").replace(">","&gt;");

        Message message = Message.builder()
                .customMessage(isCustomMessage)
                .messageBody(messageBody)
                .creationDateTime(LocalDateTime.now())
                .fromUser(tenant)
                .messageStatus(MessageStatus.UNREAD)
                .build();
        return messageMapper.toMessageModel(messageRepository.save(message));
    }

    @Override
    public void updateStatusOfDeniedDocuments(Tenant principalAuthTenant) {

        principalAuthTenant.getDocuments().forEach(document -> {
            if (document.getDocumentStatus().equals(DocumentStatus.DECLINED)) {
                document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                documentRepository.save(document);
            }
            principalAuthTenant.getGuarantors().forEach(guarantor -> {
                guarantor.getDocuments().forEach(document1 -> {
                    if (document1.getDocumentStatus().equals(DocumentStatus.DECLINED)) {
                        document1.setDocumentStatus(DocumentStatus.TO_PROCESS);
                        documentRepository.save(document1);
                    }

                });
            });

        });
    }
}
