package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.HashSet;
import fr.dossierfacile.common.entity.UserRole;

@Entity(name = "OperatorEntity")
@Table(name = "user_account")
@SQLRestriction("user_type = 'BO'")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String keycloakId;

    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "creation_date")
    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now(ZoneId.systemDefault());

    @Column(name = "last_login_date")
    @Builder.Default
    private LocalDateTime lastLoginDate = LocalDateTime.now(ZoneId.systemDefault());

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();
}
