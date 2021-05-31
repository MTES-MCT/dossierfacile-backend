package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByFromUserOrToUserOrderByCreationDateTimeDesc(User tenant, User tenant1);
}
