package fr.gouv.bo.service;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.GetEmailEventReport;
import brevoModel.GetEmailEventReportEvents;
import fr.gouv.bo.dto.BrevoMailHistoryViewDTO;
import fr.gouv.bo.dto.BrevoMailStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoMailHistoryService {

    static final int BREVO_PAGE_LIMIT = 500;
    static final int MAX_PAGES = 2;
    static final int MAX_DISPLAYED_MAILS = 25;

    private final TransactionalEmailsApi transactionalEmailsApi;

    public BrevoMailHistoryViewDTO getLast90DaysHistory(String recipientEmail) {
        if (StringUtils.isBlank(recipientEmail)) {
            return BrevoMailHistoryViewDTO.builder().items(List.of()).build();
        }

        try {
            List<GetEmailEventReportEvents> allEvents = fetchAllEvents(recipientEmail.trim());
            return BrevoMailHistoryViewDTO.builder()
                    .items(consolidateByMessageId(allEvents))
                    .build();
        } catch (ApiException e) {
            log.warn("Unable to retrieve Brevo history for recipient={} code={} message={}",
                    recipientEmail, e.getCode(), e.getMessage());
            return BrevoMailHistoryViewDTO.builder()
                    .items(List.of())
                    .error(true)
                    .errorMessage("Historique d'envoi des emails indisponible pour le moment.")
                    .build();
        } catch (Exception e) {
            log.warn("Unexpected Brevo history error for recipient={}", recipientEmail, e);
            return BrevoMailHistoryViewDTO.builder()
                    .items(List.of())
                    .error(true)
                    .errorMessage("Historique d'envoi des emails indisponible pour le moment.")
                    .build();
        }
    }

    private List<GetEmailEventReportEvents> fetchAllEvents(String recipientEmail) throws ApiException {
        String startDate = LocalDate.now().minusDays(90).toString();
        String endDate = LocalDate.now().toString();
        long offset = 0L;
        int pages = 0;
        List<GetEmailEventReportEvents> events = new ArrayList<>();

        while (pages < MAX_PAGES) {
            GetEmailEventReport report = transactionalEmailsApi.getEmailEventReport(
                    (long) BREVO_PAGE_LIMIT,
                    offset,
                    startDate,
                    endDate,
                    null,
                    recipientEmail,
                    null,
                    null,
                    null,
                    null,
                    "desc"
            );

            List<GetEmailEventReportEvents> page = report != null && report.getEvents() != null
                    ? report.getEvents()
                    : List.of();
            if (page.isEmpty()) {
                break;
            }

            events.addAll(page);
            if (page.size() < BREVO_PAGE_LIMIT) {
                break;
            }

            offset += BREVO_PAGE_LIMIT;
            pages++;
        }

        return events;
    }

    List<BrevoMailStatusDTO> consolidateByMessageId(List<GetEmailEventReportEvents> events) {
        Map<String, List<GetEmailEventReportEvents>> byMessageId = new HashMap<>();

        for (GetEmailEventReportEvents event : events) {
            String messageId = StringUtils.trimToEmpty(event.getMessageId());
            if (messageId.isEmpty()) {
                continue;
            }
            byMessageId.computeIfAbsent(messageId, ignored -> new ArrayList<>()).add(event);
        }

        return byMessageId.entrySet().stream()
                .map(entry -> toStatus(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> {
                    LocalDateTime leftDate = left.getSentAt();
                    LocalDateTime rightDate = right.getSentAt();
                    if (leftDate == null && rightDate == null) {
                        return 0;
                    }
                    if (leftDate == null) {
                        return 1;
                    }
                    if (rightDate == null) {
                        return -1;
                    }
                    return rightDate.compareTo(leftDate);
                })
                .limit(MAX_DISPLAYED_MAILS)
                .toList();
    }

    private BrevoMailStatusDTO toStatus(String messageId, List<GetEmailEventReportEvents> events) {
        GetEmailEventReportEvents latest = events.stream()
                .max(Comparator.comparing(this::safeEventDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        LocalDateTime sentAt = events.stream()
                .map(this::safeEventDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (latest == null) {
            return BrevoMailStatusDTO.builder()
                    .fromEmail("")
                    .subject("")
                    .lastEventType("")
                    .build();
        }

        return BrevoMailStatusDTO.builder()
                .sentAt(sentAt)
                .fromEmail(StringUtils.defaultString(latest.getFrom()))
                .subject(StringUtils.defaultString(latest.getSubject()))
                .lastEventType(toFrenchStatus(latest.getEvent() != null ? latest.getEvent().getValue() : ""))
                .lastEventAt(safeEventDate(latest))
                .build();
    }

    private String toFrenchStatus(String status) {
        return switch (StringUtils.lowerCase(StringUtils.trimToEmpty(status))) {
            case "opened" -> "ouvert";
            case "clicks" -> "cliqué";
            case "delivered" -> "délivré";
            case "deferred" -> "différé";
            case "hardBounces" -> "rebond dur";
            case "softBounces" -> "rebond léger";
            case "invalid" -> "invalide";
            case "blocked" -> "bloqué";
            case "spam" -> "spam";
            case "error" -> "erreur";
            case "requests" -> "envoyé";
            case "unsubscribed" -> "désabonné";
            case "bounces" -> "rebond";
            case "loadedByProxy" -> "chargé par proxy";
            default -> status;
        };
    }

    private LocalDateTime safeEventDate(GetEmailEventReportEvents event) {
        String rawDate = event.getDate();
        if (StringUtils.isBlank(rawDate)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawDate).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(rawDate);
            } catch (DateTimeParseException e) {
                log.debug("Unable to parse Brevo event date={}", rawDate);
                return null;
            }
        }
    }
}
