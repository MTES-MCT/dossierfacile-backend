package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.LinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "link_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkLog {

    private static final long serialVersionUID = -3603846262626208324L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    private UUID token;

    @Column
    @Enumerated(EnumType.STRING)
    private LinkType linkType;

    private LocalDateTime creationDate;

    @Column
    private String ipAddress;

    public LinkLog(ApartmentSharing apartmentSharing, UUID token, LinkType linkType, String ip) {
        this.apartmentSharing = apartmentSharing;
        this.token = token;
        this.linkType = linkType;
        this.ipAddress = ip;
        creationDate = LocalDateTime.now();
    }

}
