package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.enums.ActionOperatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperatorLogRepository extends JpaRepository<OperatorLog, Long> {

    Long countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(Long operatorId, ActionOperatorType actionType, LocalDateTime startDate);

    boolean existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
            Long operatorId,
            Long tenantId,
            List<ActionOperatorType> types,
            LocalDateTime since
    );

    @Query("""
            SELECT COUNT(ol) > 0 FROM OperatorLog ol
            WHERE ol.operator.id = :operatorId
              AND ol.tenant.apartmentSharing.id = :apartmentSharingId
              AND ol.actionOperatorType IN :types
              AND ol.creationDate >= :since
            """)
    boolean existsAssignmentForApartmentSharing(
            @Param("operatorId") Long operatorId,
            @Param("apartmentSharingId") Long apartmentSharingId,
            @Param("types") List<ActionOperatorType> types,
            @Param("since") LocalDateTime since
    );
}
