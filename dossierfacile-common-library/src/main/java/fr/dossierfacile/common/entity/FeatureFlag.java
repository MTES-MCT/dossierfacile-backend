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
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Partner-scoped flags: comma-separated list of {@code user_api.name} (Keycloak client
     * ids) the flag applies to. Null or blank means no partner.
     */
    @Column(name = "opted_in_partners", columnDefinition = "TEXT")
    private String optedInPartners;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    @UpdateTimestamp
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    @OneToMany(mappedBy = "featureFlag", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private Set<UserFeatureAssignment> assignments = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "featureFlag", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private Set<UserFeatureAssignmentHistory> assignmentHistory = new HashSet<>();

    /**
     * Partner names of {@link #optedInPartners}, trimmed, without blanks nor duplicates,
     * in declaration order.
     */
    public Set<String> getOptedInPartnerNames() {
        if (optedInPartners == null || optedInPartners.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(optedInPartners.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String joinPartnerNames(Collection<String> names) {
        return names == null ? null : names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

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
