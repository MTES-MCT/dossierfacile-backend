package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Prospect;
import fr.dossierfacile.common.enums.TenantType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {

    List<Prospect> findAllByPropertyAndProspectType(Property property, TenantType type);

    List<Prospect> findAllByEventId(String eventId);

    List<Prospect> findAllByEmail(String email);

    List<Prospect> findProspectByVisitDateBetweenAndReminderEmailVisitIsFalse(LocalDateTime initDate, LocalDateTime endDate);

    List<Prospect> findProspectByReminderWelcomeIsFalseAndCreationDateTimeBetween(LocalDateTime initDate, LocalDateTime endDate);

    Prospect findFirst1ByEmailAndProperty(String email, Property property);
}
