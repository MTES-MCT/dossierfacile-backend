package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.TenantUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantCommonRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByEmail(String email);
    Optional<Tenant> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    @Query("select distinct t FROM Tenant t where concat(LOWER(t.firstName),' ',LOWER(t.lastName)) like CONCAT('%',:nameUser,'%') ")
    List<Tenant> findTenantByFirstNameOrLastNameOrFullName(@Param("nameUser") String nameUser);

    @Query("select t from Tenant t " +
            " where (t.operatorDateTime is null or t.operatorDateTime < :localDateTime)" +
            " and t.status = 'TO_PROCESS' and t.honorDeclaration = true " +
            " order by t.lastUpdateDate")
    Page<Tenant> findTenantsToProcess(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);

    @Query("""
            SELECT t
            FROM Tenant t
            WHERE (t.operatorDateTime IS NULL OR t.operatorDateTime < :localDateTime)
              AND t.status = 'TO_PROCESS'
              AND t.honorDeclaration = true
              AND NOT EXISTS ( SELECT 1 FROM Tenant t2 JOIN t2.documents d WHERE t2.id = t.id AND d.watermarkFile IS NULL )
              AND NOT EXISTS ( SELECT 1 FROM Tenant t3 JOIN t3.guarantors g JOIN g.documents d2 WHERE t3.id = t.id AND d2.watermarkFile IS NULL )
            ORDER BY t.lastUpdateDate
            """)
    Page<Tenant> findNextApplication(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);

    default Tenant findNextApplication(LocalDateTime localDateTime) {
        Page<Tenant> page = findNextApplication(localDateTime, PageRequest.of(0, 1, Sort.Direction.ASC, "lastUpdateDate"));
        if (!page.isEmpty()) {
            return page.get().findFirst().orElse(null);
        }
        return null;
    }

    @Query("select t from Tenant t " +
            " where (t.operatorDateTime is null or t.operatorDateTime < :localDateTime) and t.status = 'TO_PROCESS' and t.honorDeclaration = true " +
            " and" +
            " t.id not in (select t.id from Tenant t join t.documents d WHERE d.watermarkFile is null)" +
            " and" +
            " t.id in (select t.id from Tenant t join t.documents d WHERE d.documentSubCategory IN (:categories) )" +
            " and" +
            " t.id not in (select t.id from Tenant t join t.guarantors g join g.documents d WHERE d.watermarkFile is null)" +
            " order by t.lastUpdateDate")
    Page<Tenant> findNextApplicationByProfessional(@Param("localDateTime") LocalDateTime localDateTime, @Param("categories") List<DocumentSubCategory> categories, Pageable pageable);


    default Tenant findNextApplicationByProfessional(LocalDateTime localDateTime, List<DocumentSubCategory> categories) {
        Page<Tenant> page = findNextApplicationByProfessional(localDateTime, categories, PageRequest.of(0, 1, Sort.Direction.ASC, "lastUpdateDate"));
        if (!page.isEmpty()) {
            return page.get().findFirst().orElse(null);
        }
        return null;
    }

    Page<Tenant> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String q, String q1, String q2, Pageable pageable);

    @Query("""
            SELECT t
            FROM Tenant t
            WHERE t.status = 'TO_PROCESS'
              AND t.honorDeclaration = true
            ORDER BY t.lastUpdateDate ASC
            """)
    Page<Tenant> findToProcessApplicationsByOldestUpdateDate(Pageable pageable);

    List<Tenant> findAllByApartmentSharingId(Long ap);

    Tenant findOneById(Long id);

    @Query(
            value = "select distinct tl.tenant_id from tenant_log as tl \n" +
                    "left join tenant_userapi tu on tl.tenant_id = tu.tenant_id \n" +
                    "where tu.userapi_id=:id and tl.log_type = 'ACCOUNT_VALIDATED' \n" +
                    "and tl.creation_date >= :since",
            nativeQuery = true
    )
    List<Long> listIdTenantsAccountCompletedPendingToSendCallBack(@Param("id") Long userApiId, @Param("since") LocalDateTime lastUpdateSince);

    @Query("SELECT t FROM Tenant t ORDER BY t.id DESC")
    Page<Tenant> findAllTenants(Pageable page);

    @Query(value = """
            SELECT COUNT(DISTINCT t.id)
            FROM tenant t
            LEFT JOIN document d ON d.tenant_id = t.id OR d.guarantor_id = t.id
            WHERE d.watermark_file_id IS NULL
              AND d.processing_start_time IS NOT NULL
              AND d.processing_end_time IS NULL
              AND d.processing_start_time < NOW() - INTERVAL '12' HOUR
            """, nativeQuery = true)
    long countAllTenantsWithFailedGeneratedPdfDocument();

    @Query(value = """
            SELECT *
            FROM tenant t
            JOIN user_account u ON t.id = u.id
            WHERE t.status = 'TO_PROCESS' AND t.id IN (
              SELECT t2.id
              FROM tenant t2
              JOIN document d ON d.tenant_id = t2.id
              WHERE d.watermark_file_id IS NULL
                AND d.processing_start_time IS NOT NULL
                AND d.processing_end_time IS NULL
                AND d.processing_start_time < NOW() - INTERVAL '12' HOUR
              UNION DISTINCT
              SELECT t3.id
              FROM tenant t3
              JOIN guarantor g ON g.tenant_id = t3.id
              JOIN document d ON d.guarantor_id = g.id
              WHERE d.watermark_file_id IS NULL
                AND d.processing_start_time IS NOT NULL
                AND d.processing_end_time IS NULL
                AND d.processing_start_time < NOW() - INTERVAL '12' HOUR
            )
            ORDER BY t.last_update_date DESC
            """, nativeQuery = true)
    Page<Tenant> findAllTenantsToProcessWithFailedGeneratedPdfDocument(Pageable pageable);

    long countAllByStatus(TenantFileStatus tenantFileStatus);
    //endregion

    //region Used in FO
    Optional<Tenant> findByEmailAndEnabledFalse(String email);

    @Query("from Tenant t where t.email in :emails")
    List<Tenant> findByListEmail(@Param("emails") List<String> emails);

    List<Tenant> findAllByEnabledIsFalseAndCreationDateTimeIsBetween(LocalDateTime initDate, LocalDateTime endDate);

    @Query("from Tenant t " +
            "join Log l on t.id = l.tenantId " +
            "where t.honorDeclaration = false and l.logType = 'EMAIL_ACCOUNT_VALIDATED' and l.creationDateTime between :initDate and :endDate")
    List<Tenant> findAllByHonorDeclarationIsFalseAndCompletionDateTimeIsBetween(@Param("initDate") LocalDateTime initDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "select distinct t2 from Tenant t2 " +
                    " join fetch t2.apartmentSharing a " +
                    " join fetch a.tenants ts " +
                    " join Log l on t2.id = l.tenantId " +
                    " where " +
                    " t2.lastUpdateDate < :startDate " +
                    " and " +
                    " l.creationDateTime between :startDate and :endDate " +
                    " and " +
                    " l.logType = 'ACCOUNT_DENIED' " +
                    " and " +
                    " ( " +
                    "   t2.id in ( " +
                    "        select t.id from Tenant t" +
                    "        join t.documents d" +
                    "        join t.apartmentSharing a " +
                    "        where d.documentStatus = 'DECLINED'" +
                    "   ) " +
                    "   or " +
                    "   t2.id in (" +
                    "        select t.id from Tenant t" +
                    "        join t.guarantors g" +
                    "        join g.documents d" +
                    "        join t.apartmentSharing a" +
                    "        where d.documentStatus = 'DECLINED'" +
                    "   ) " +
                    " )"
    )
    List<Tenant> findAllDeclinedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT COUNT(t.id)
            FROM tenant t
            LEFT JOIN document d ON d.tenant_id = t.id OR d.guarantor_id = t.id
            WHERE t.apartment_sharing_id = :apartmentSharingId
              AND ( t.status != 'VALIDATED' OR d.watermark_file_id IS NULL )
            """, nativeQuery = true)
    int countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(@Param("apartmentSharingId") long apartmentSharingId);

    @Query("""
            SELECT DISTINCT t
            FROM Tenant t
            JOIN Log l ON t.id = l.tenantId
            LEFT JOIN TenantUserApi tu ON t.id = tu.tenant.id
            WHERE l.creationDateTime BETWEEN :startDate AND :endDate
              AND l.logType = 'ACCOUNT_VALIDATED'
            """
    )
    List<Tenant> findAllTenantsValidatedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "from Tenant t " +
            "where t.lastLoginDate < :localDateTime " +
            "and t.warnings = :warnings " +
            "and t.id in (select d.tenant.id from Document d where d.tenant.id is not null)")
    Page<Tenant> findByLastLoginDateIsBeforeAndHasDocuments(Pageable pageable, @Param("localDateTime") LocalDateTime localDateTime, @Param("warnings") Integer warnings);

    @Query(value = "select count(*) from Tenant t " +
            "where t.lastLoginDate < :localDateTime " +
            "and t.warnings = :warnings " +
            "and t.id in (select d.tenant.id from Document d where d.tenant.id is not null)")
    long countByLastLoginDateIsBeforeAndHasDocuments(@Param("localDateTime") LocalDateTime localDateTime, @Param("warnings") Integer warnings);

    @Modifying
    @Query("UPDATE Tenant t SET t.warnings = 0 where t.id = :tenantId")
    void resetWarnings(@Param("tenantId") Long tenantId);

    List<Tenant> findAllByApartmentSharing(ApartmentSharing apartmentSharing);

    List<Tenant> findByEmailInAndApartmentSharingNot(List<String> coTenantEmail, ApartmentSharing apartmentSharing);

    Tenant findByKeycloakId(String keycloakId);

    @Query(value = """
            SELECT t.id as id, t.apartment_sharing_id as apartmentSharingId, t.last_update_date as lastUpdateDate, ua.creation_date as creationDate 
            FROM  tenant t
            INNER JOIN user_account ua ON ua.id = t.id
            INNER JOIN tenant_userapi tua ON tua.tenant_id = t.id  
            WHERE tua.userapi_id = :partnerId
            AND ( CAST( CAST(:lastUpdateFrom AS text) AS timestamp) IS NULL OR t.last_update_date > CAST( CAST(:lastUpdateFrom AS text) AS timestamp))
            ORDER BY t.last_update_date ASC
            LIMIT :limit
            """, nativeQuery = true
    )
    List<TenantUpdate> findTenantUpdateByLastUpdateAndPartner(@Param("lastUpdateFrom") LocalDateTime from, @Param("partnerId") Long id, @Param("limit") Long limit);

    @Query(value = """
            SELECT t.id as id, t.apartment_sharing_id as apartmentSharingId, t.last_update_date as lastUpdateDate, ua.creation_date as creationDate 
            FROM  user_account ua 
            INNER JOIN tenant t ON t.id = ua.id
            INNER JOIN tenant_userapi tua ON tua.tenant_id = t.id  
            WHERE tua.userapi_id = :partnerId
            AND ( CAST( CAST(:creationDateFrom AS text) AS timestamp) IS NULL OR ua.creation_date > CAST( CAST(:creationDateFrom AS text) AS timestamp))
            ORDER BY ua.creation_date ASC
            LIMIT :limit
            """, nativeQuery = true
    )
    List<TenantUpdate> findTenantUpdateByCreationDateAndPartner(@Param("creationDateFrom") LocalDateTime from, @Param("partnerId") Long id, @Param("limit") Long limit);

    @Query("FROM Tenant t WHERE t.status = :status AND t.lastUpdateDate < :before")
    List<Tenant> findByStatusAndLastUpdateDate(@Param("status") TenantFileStatus status, @Param("before") LocalDateTime before, Pageable pageable);

    @Query("""
            SELECT DISTINCT t
            FROM Tenant t
            JOIN Log l ON t.id = l.tenantId
            WHERE l.logType = 'ACCOUNT_VALIDATED' OR l.logType = 'ACCOUNT_DENIED' 
            """
    )
    List<Tenant> findTenantsToExtract( Pageable pageable);
}
