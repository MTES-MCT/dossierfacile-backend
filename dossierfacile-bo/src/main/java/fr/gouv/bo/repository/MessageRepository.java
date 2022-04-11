package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByFromUserOrToUserOrderByCreationDateTimeDesc(User tenant, User tenant1);

    @Modifying
    @Query("UPDATE Message m SET m.messageStatus = 'READ' where m.fromUser = :tenant")
    void markReadAdmin(@Param("tenant") User tenant);

    List<Message> findByMessageStatusAndToUser(MessageStatus messageStatus, User tenant);

    List<Message> findByMessageStatusAndFromUser(MessageStatus messageStatus, User tenant);

    Message findFirstByToUserAndCustomMessageOrderByCreationDateTimeDesc(User user, boolean b);

    Message findFirstByFromUserAndCustomMessageOrderByCreationDateTimeDesc(User user, boolean b);
}
