package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.UserFeatureAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFeatureAssignmentHistoryRepository extends JpaRepository<UserFeatureAssignmentHistory, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO user_feature_assignment_history (user_id, feature_key, enabled, bucket, rollout_pct, reason, changed_at)
            SELECT user_id, feature_key, (bucket < :newRolloutPct), bucket, :newRolloutPct, :reason, now()
            FROM user_feature_assignment
            WHERE feature_key = :featureKey
              AND assignment_source = 'HASH'
              AND enabled != (bucket < :newRolloutPct)
            """, nativeQuery = true)
    void saveHistoryForChangingAssignments(@Param("featureKey") String featureKey, @Param("newRolloutPct") Integer newRolloutPct, @Param("reason") String reason);

}
