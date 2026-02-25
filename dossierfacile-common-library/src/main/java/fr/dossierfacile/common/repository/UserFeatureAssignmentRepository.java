package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.entity.UserFeatureAssignment;
import fr.dossierfacile.common.entity.UserFeatureAssignmentId;
import fr.dossierfacile.common.enums.FeatureAssignmentSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFeatureAssignmentRepository extends JpaRepository<UserFeatureAssignment, UserFeatureAssignmentId> {
    List<UserFeatureAssignment> findByFeatureFlag(FeatureFlag featureFlag);

    @Modifying
    @Query("UPDATE UserFeatureAssignment u SET u.rolloutPct = :newRolloutPct, u.enabled = CASE WHEN u.bucket < :newRolloutPct THEN true ELSE false END WHERE u.featureFlag.key = :featureKey AND u.assignmentSource = :source")
    void updateRolloutPctAndEnabled(@Param("featureKey") String featureKey, @Param("newRolloutPct") Integer newRolloutPct, @Param("source") FeatureAssignmentSource source);
}
