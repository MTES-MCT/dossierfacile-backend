package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.FeatureAssignmentSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_feature_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserFeatureAssignment implements Serializable {

    @Serial
    private static final long serialVersionUID = 2401587134549023878L;

    @EmbeddedId
    private UserFeatureAssignmentId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @MapsId("featureKey")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_key", nullable = false)
    @ToString.Exclude
    private FeatureFlag featureFlag;

    @Builder.Default
    private boolean enabled = false;

    @Column(nullable = false)
    private Integer bucket;

    @Column(name = "rollout_pct", nullable = false)
    private Integer rolloutPct;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_source", length = 20, nullable = false)
    private FeatureAssignmentSource assignmentSource;

    @PrePersist
    void onPersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (id == null && user != null && featureFlag != null) {
            id = new UserFeatureAssignmentId(user.getId(), featureFlag.getKey());
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
        UserFeatureAssignment that = (UserFeatureAssignment) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
