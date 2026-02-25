package fr.dossierfacile.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "feature_flag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FeatureFlag implements Serializable {

    @Serial
    private static final long serialVersionUID = -595089775012345987L;

    @Id
    @Column(name = "key", length = 100, nullable = false)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean active = false;

    @Builder.Default
    @Column(name = "only_for_new_user", nullable = false)
    private boolean onlyForNewUser = true;

    @Column(name = "rollout_pct", nullable = false)
    @Builder.Default
    private Integer rolloutPct = 0;

    @Column(name = "deployment_date")
    private LocalDateTime deploymentDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "featureFlag", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private Set<UserFeatureAssignment> assignments = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "featureFlag", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private Set<UserFeatureAssignmentHistory> assignmentHistory = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        FeatureFlag that = (FeatureFlag) o;
        return key != null && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}
