package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface CompletedOptinTenantRepository extends JpaRepository<Tenant, Long> {

    // COMPLETED tenants who never answered the verification question, whose last submission
    // (ACCOUNT_COMPLETED log) is inside the window and who have not been reminded yet.
    // Driven by the log window so that the creation_date index bounds the scan.
    @Query("""
            select distinct t.id from Tenant t
            join TenantLog l on l.tenantId = t.id
            where l.logType = 'ACCOUNT_COMPLETED'
            and l.creationDateTime >= :submittedFrom
            and l.creationDateTime < :submittedBefore
            and t.status = 'COMPLETED'
            and t.validationRequested is null
            and not exists (
                select 1 from TenantLog later
                where later.tenantId = t.id
                and later.logType = 'ACCOUNT_COMPLETED'
                and later.creationDateTime >= :submittedBefore
            )
            and not exists (
                select 1 from TenantLog reminder
                where reminder.tenantId = t.id
                and reminder.logType = 'VALIDATION_REMINDER_SENT'
            )
            """)
    List<Long> findTenantIdsToRemindForValidation(@Param("submittedFrom") LocalDateTime submittedFrom,
                                                  @Param("submittedBefore") LocalDateTime submittedBefore);

    // Tenants still COMPLETED whose last submission (ACCOUNT_COMPLETED log) is inside the window
    // and who have not received the impact survey yet
    @Query("""
            select distinct t.id from Tenant t
            join TenantLog l on l.tenantId = t.id
            where l.logType = 'ACCOUNT_COMPLETED'
            and l.creationDateTime >= :submittedFrom
            and l.creationDateTime < :submittedBefore
            and t.status = 'COMPLETED'
            and not exists (
                select 1 from TenantLog later
                where later.tenantId = t.id
                and later.logType = 'ACCOUNT_COMPLETED'
                and later.creationDateTime >= :submittedBefore
            )
            and not exists (
                select 1 from TenantLog survey
                where survey.tenantId = t.id
                and survey.logType = 'COMPLETED_SURVEY_SENT'
            )
            """)
    List<Long> findTenantIdsForCompletedSurvey(@Param("submittedFrom") LocalDateTime submittedFrom,
                                               @Param("submittedBefore") LocalDateTime submittedBefore);

}
