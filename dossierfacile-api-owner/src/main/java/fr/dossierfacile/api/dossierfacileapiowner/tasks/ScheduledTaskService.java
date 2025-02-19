package fr.dossierfacile.api.dossierfacileapiowner.tasks;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.property.PropertyRepository;
import fr.dossierfacile.common.entity.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final MailService mailService;
    private final PropertyRepository propertyRepository;

    @Value("${days_after_validated_property_to_follow_up_email:42}")
    private Long daysAfterValidatedPropertyToFollowUpEmail;

    @Scheduled(cron = "0 10 2 * * ?")
    void sendFollowUpValidatedPropertyAfter() {
        log.info("sendFollowUpValidatedPropertyAfter is launched");
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysAfterValidatedPropertyToFollowUpEmail).with(LocalTime.MIN);
        LocalDateTime endDate = LocalDateTime.now().minusDays(daysAfterValidatedPropertyToFollowUpEmail).with(LocalTime.MAX);

        List<Property> properties = propertyRepository.findAllByValidatedIsTrueAndValidatedDateIsBetween(startDate, endDate);
        for (Property property : properties) {
            mailService.sendEmailFollowUpValidatedProperty(property.getOwner(), property);
        }
    }
}
