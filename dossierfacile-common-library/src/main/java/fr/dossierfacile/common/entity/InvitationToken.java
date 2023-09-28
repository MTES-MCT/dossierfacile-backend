package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_token")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvitationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_date")
    private LocalDateTime created_date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    @ToString.Exclude
    private ApartmentSharing apartmentSharing;

    @Column(nullable = false, length = 96)
    private String token;

    @Column(nullable = false, length = 255)
    private String email;

}
