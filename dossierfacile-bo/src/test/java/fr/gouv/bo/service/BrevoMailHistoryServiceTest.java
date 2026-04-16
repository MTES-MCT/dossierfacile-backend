package fr.gouv.bo.service;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.GetEmailEventReport;
import brevoModel.GetEmailEventReportEvents;
import fr.gouv.bo.dto.BrevoMailHistoryViewDTO;
import fr.gouv.bo.dto.BrevoMailStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrevoMailHistoryServiceTest {

    private TransactionalEmailsApi transactionalEmailsApi;
    private BrevoMailHistoryService service;

    @BeforeEach
    void setUp() {
        transactionalEmailsApi = mock(TransactionalEmailsApi.class);
        service = new BrevoMailHistoryService(transactionalEmailsApi);
    }

    @Test
    void getLast90DaysHistory_returnsConsolidatedLastEventByMessageId() throws Exception {
        GetEmailEventReportEvents delivered = event("m-1", "Paiement", "delivered", "2026-04-09T10:00:00+00:00", "noreply@example.com");
        GetEmailEventReportEvents opened = event("m-1", "Paiement", "opened", "2026-04-09T10:05:00+00:00", "noreply@example.com");
        GetEmailEventReportEvents bounced = event("m-2", "Compte", "hardBounces", "2026-04-08T09:00:00+00:00", "noreply@example.com");
        GetEmailEventReport report = new GetEmailEventReport().events(List.of(delivered, opened, bounced));

        when(transactionalEmailsApi.getEmailEventReport(
                any(), any(), any(), any(), any(), eq("john@example.com"), any(), any(), any(), any(), any()
        )).thenReturn(report);

        BrevoMailHistoryViewDTO history = service.getLast90DaysHistory("john@example.com");

        assertThat(history.isError()).isFalse();
        assertThat(history.getItems()).hasSize(2);
        BrevoMailStatusDTO first = history.getItems().getFirst();
        BrevoMailStatusDTO second = history.getItems().get(1);
        assertThat(first.getFromEmail()).isEqualTo("noreply@example.com");
        assertThat(first.getLastEventType()).isEqualTo("ouvert");
        assertThat(first.getSentAt()).isNotNull();
        assertThat(first.getSentAt()).isAfter(second.getSentAt());
    }

    @Test
    void getLast90DaysHistory_ignoresEventsWithoutMessageId() throws Exception {
        GetEmailEventReportEvents noId = event("", "Sans ID", "opened", "2026-04-09T10:05:00+00:00", "noreply@example.com");
        GetEmailEventReportEvents withId = event("m-3", "Avec ID", "delivered", "2026-04-09T10:00:00+00:00", "noreply@example.com");
        GetEmailEventReport report = new GetEmailEventReport().events(List.of(noId, withId));

        when(transactionalEmailsApi.getEmailEventReport(
                any(), any(), any(), any(), any(), eq("john@example.com"), any(), any(), any(), any(), any()
        )).thenReturn(report);

        BrevoMailHistoryViewDTO history = service.getLast90DaysHistory("john@example.com");

        assertThat(history.getItems()).hasSize(1);
        assertThat(history.getItems().getFirst().getSubject()).isEqualTo("Avec ID");
    }

    @Test
    void getLast90DaysHistory_returnsErrorStateWhenBrevoFails() throws Exception {
        when(transactionalEmailsApi.getEmailEventReport(
                any(), any(), any(), any(), any(), eq("john@example.com"), any(), any(), any(), any(), any()
        )).thenThrow(new ApiException("boom"));

        BrevoMailHistoryViewDTO history = service.getLast90DaysHistory("john@example.com");

        assertThat(history.isError()).isTrue();
        assertThat(history.getItems()).isEmpty();
    }

    @Test
    void getLast90DaysHistory_returnsEmptyWhenEmailMissing() {
        BrevoMailHistoryViewDTO history = service.getLast90DaysHistory(" ");

        assertThat(history.isError()).isFalse();
        assertThat(history.getItems()).isEmpty();
    }

    @Test
    void getLast90DaysHistory_limitsResultsTo25Mails() throws Exception {
        List<GetEmailEventReportEvents> events = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            OffsetDateTime date = OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC).minusDays(i);
            events.add(event(
                    "message-" + i,
                    "Sujet " + i,
                    "delivered",
                    date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "noreply@example.com"
            ));
        }

        GetEmailEventReport report = new GetEmailEventReport().events(events);
        when(transactionalEmailsApi.getEmailEventReport(
                any(), any(), any(), any(), any(), eq("john@example.com"), any(), any(), any(), any(), any()
        )).thenReturn(report);

        BrevoMailHistoryViewDTO history = service.getLast90DaysHistory("john@example.com");

        assertThat(history.getItems()).hasSize(25);
    }

    private GetEmailEventReportEvents event(String messageId, String subject, String eventType, String date, String from) {
        return new GetEmailEventReportEvents()
                .messageId(messageId)
                .subject(subject)
                .event(GetEmailEventReportEvents.EventEnum.fromValue(eventType))
                .from(from)
                .date(date);
    }
}
