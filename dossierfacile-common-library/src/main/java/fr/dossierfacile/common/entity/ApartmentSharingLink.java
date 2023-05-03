package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apartment_sharing_link")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApartmentSharingLink implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    @Column
    private String token;

    @Column
    private boolean fullData = false;

    @Column
    private boolean disabled = false;

    @Column
    @Enumerated(EnumType.STRING)
    private ApartmentSharingLinkType linkType;

    @Column
    private boolean mailSent = false;

    @Column
    private String email;

}
