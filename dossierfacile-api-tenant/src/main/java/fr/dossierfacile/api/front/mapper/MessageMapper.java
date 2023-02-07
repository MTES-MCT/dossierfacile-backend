package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.model.TypeMessage;
import fr.dossierfacile.common.entity.Message;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper(componentModel = "spring")
public interface MessageMapper {
    List<MessageModel> toListMessageModel(List<Message> message);

    @Mapping(source = "fromUser.id", target = "fromUser")
    @Mapping(source = "toUser.id", target = "toUser")
    @Mapping(source = "messageStatus", target = "status")
    MessageModel toMessageModel(Message message);

    @AfterMapping
    default void typeMessage(@MappingTarget MessageModel.MessageModelBuilder messageModelBuilder) {
        MessageModel messageModel = messageModelBuilder.build();
        messageModelBuilder.typeMessage(messageModel.getFromUser() != null ? TypeMessage.FROM_TENANT : TypeMessage.TO_TENANT);
    }
}
