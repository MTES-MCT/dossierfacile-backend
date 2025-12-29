package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentSharingLinkEnrichedDTO {
    
    private Long id;
    private UUID token;
    private String title;
    private LocalDateTime creationDate;
    private LocalDateTime expirationDate;
    private LocalDateTime lastSentDatetime;
    private boolean fullData;
    private boolean disabled;
    private ApartmentSharingLinkType linkType;
    private String email;
    private Long partnerId;
    private String partnerName;
    
    // Creator info
    private Long createdBy;
    private String createdByName;
    
    // Visit statistics
    private long nbVisits;
    private LocalDateTime firstVisit;
    private LocalDateTime lastVisit;
    
    // Access logs
    private List<LinkLog> accessLogs;
    
    // Complete URL
    private String fullUrl;
    
    public static ApartmentSharingLinkEnrichedDTO fromEntity(ApartmentSharingLink link) {
        return ApartmentSharingLinkEnrichedDTO.builder()
                .id(link.getId())
                .token(link.getToken())
                .title(link.getTitle())
                .creationDate(link.getCreationDate())
                .expirationDate(link.getExpirationDate())
                .lastSentDatetime(link.getLastSentDatetime())
                .fullData(link.isFullData())
                .disabled(link.isDisabled())
                .linkType(link.getLinkType())
                .email(link.getEmail())
                .partnerId(link.getPartnerId())
                .createdBy(link.getCreatedBy())
                .build();
    }
}

