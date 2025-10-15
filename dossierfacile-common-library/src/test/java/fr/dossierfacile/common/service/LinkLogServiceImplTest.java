package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkLogServiceImplTest {

    private final LinkLogRepository repository = mock(LinkLogRepository.class);
    private final LinkLogService service = new LinkLogServiceImpl(repository);

    @Test
    void should_find_last_visit_date() {
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 12, 0);
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().id(1L).build();
        UUID token = UUID.randomUUID();

        when(repository.findByApartmentSharingAndToken(apartmentSharing, token)).thenReturn(List.of(
                log(LinkType.FULL_APPLICATION, date.plusHours(1)),
                log(LinkType.REBUILT_TOKENS, date.plusHours(2)),
                log(LinkType.LIGHT_APPLICATION, date.plusHours(3)),
                log(LinkType.DOCUMENT, date.plusHours(4)),
                log(LinkType.DELETED_LINK_TOKEN, date.plusHours(5))
        ));

        assertThat(service.getLastVisit(token, apartmentSharing))
                .isPresent()
                .contains(date.plusHours(4));
    }

    @Test
    void should_find_no_visit() {
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 12, 0);
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().id(1L).build();
        UUID token = UUID.randomUUID();

        when(repository.findByApartmentSharingAndToken(apartmentSharing, token)).thenReturn(List.of(
                log(LinkType.REBUILT_TOKENS, date.plusHours(1)),
                log(LinkType.REBUILT_TOKENS, date.plusHours(2)),
                log(LinkType.DELETED_LINK_TOKEN, date.plusHours(3))
        ));

        assertThat(service.getLastVisit(token, apartmentSharing)).isEmpty();
    }

    private static LinkLog log(LinkType logType, LocalDateTime creationDate) {
        return LinkLog.builder()
                .linkType(logType)
                .creationDate(creationDate)
                .build();
    }

}