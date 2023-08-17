package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.LinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

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

    private String token;

    @Column
    @Enumerated(EnumType.STRING)
    private LinkType linkType;

    private LocalDateTime creationDate;

    public LinkLog(ApartmentSharing apartmentSharing, String token, LinkType linkType) {
        this.apartmentSharing = apartmentSharing;
        this.token = token;
        this.linkType = linkType;
        creationDate = LocalDateTime.now();
    }

}
