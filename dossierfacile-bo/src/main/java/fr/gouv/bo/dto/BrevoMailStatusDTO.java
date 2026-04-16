package fr.gouv.bo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrevoMailStatusDTO {
    private LocalDateTime sentAt;
    private String fromEmail;
    private String subject;
    private String lastEventType;
    private LocalDateTime lastEventAt;
}
