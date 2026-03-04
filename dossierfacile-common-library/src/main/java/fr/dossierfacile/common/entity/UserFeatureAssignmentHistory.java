package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.FeatureAssignmentReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_feature_assignment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserFeatureAssignmentHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = -5759034902234873219L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_key", nullable = false)
    @ToString.Exclude
    private FeatureFlag featureFlag;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Integer bucket;

    @Column(name = "rollout_pct", nullable = false)
    private Integer rolloutPct;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private FeatureAssignmentReason reason;

    @PrePersist
    void onPersist() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserFeatureAssignmentHistory that = (UserFeatureAssignmentHistory) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

