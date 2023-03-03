package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
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

    boolean existsByEmail(String email);

    @Query("select distinct t FROM Tenant t where concat(LOWER(t.firstName),' ',LOWER(t.lastName)) like CONCAT('%',:nameUser,'%') ")
    List<Tenant> findTenantByFirstNameOrLastNameOrFullName(@Param("nameUser") String nameUser);

    @Query("select t from Tenant t " +
            " where (t.operatorDateTime is null or t.operatorDateTime < :localDateTime) and t.status = 'TO_PROCESS' and t.honorDeclaration = true " +
            " order by t.lastUpdateDate")
    Page<Tenant> findTenantsToProcess(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);

    @Query("select t from Tenant t " +
            " where (t.operatorDateTime is null or t.operatorDateTime < :localDateTime) and t.status = 'TO_PROCESS' and t.honorDeclaration = true " +
            " and" +
            " t.id not in (select t.id from Tenant t join t.documents d WHERE d.name is null)" +
            " and" +
            " t.id not in (select t.id from Tenant t join t.guarantors g join g.documents d WHERE d.name is null)" +
            " order by t.lastUpdateDate")
    Page<Tenant> findNextApplication(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);

    default Tenant findNextApplication(LocalDateTime localDateTime) {
        Page<Tenant> page = findNextApplication(localDateTime, PageRequest.of(0, 1, Sort.Direction.ASC, "lastUpdateDate"));
        if (!page.isEmpty()) {
            return page.get().findFirst().orElse(null);
        }
        return null;
    }

    @Query(value = "select sum(count)\n" +
            "from (SELECT COUNT(upload1) AS count FROM tenant where upload1 is not null\n" +
            "      UNION ALL SELECT COUNT(upload2) AS count FROM tenant where upload2 is not null\n" +
            "      UNION ALL SELECT COUNT(upload3) AS count FROM tenant where upload3 is not null\n" +
            "      UNION ALL SELECT COUNT(upload4) AS count FROM tenant where upload4 is not null\n" +
            "      UNION ALL SELECT COUNT(upload5) AS count FROM tenant where upload5 is not null\n" +
            "      UNION ALL SELECT COUNT(upload1) AS count FROM guarantor where upload1 is not null\n" +
            "      UNION ALL SELECT COUNT(upload2) AS count FROM guarantor where upload2 is not null\n" +
            "      UNION ALL SELECT COUNT(upload3) AS count FROM guarantor where upload3 is not null\n" +
            "      UNION ALL SELECT COUNT(upload4) AS count FROM guarantor where upload4 is not null\n" +
            "      UNION ALL SELECT COUNT(upload5) AS count FROM guarantor where upload5 is not null) as total", nativeQuery = true)
    Long countTotalUploadedFiles();

    Page<Tenant> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String q, String q1, String q2, Pageable pageable);

    List<Tenant> findAllByApartmentSharingId(Long ap);

    Tenant findOneById(Long id);

    @Query(
            value = "select distinct tl.tenant_id from tenant_log as tl \n" +
                    "left join tenant_userapi tu on tl.tenant_id = tu.tenant_id \n" +
                    "where tu.userapi_id=:id and tl.log_type = 'ACCOUNT_VALIDATED'",
            nativeQuery = true
    )
    List<Long> listIdTenantsAccountCompletedPendingToSendCallBack(@Param("id") Long userApiId);

    @Query("SELECT t FROM Tenant t ORDER BY t.id DESC")
    Page<Tenant> findAllTenants(Pageable page);

    @Query(value = "SELECT count(distinct t.id) from tenant t where t.id in (SELECT t2.id from tenant t2 join document d " +
            "on d.tenant_id = t2.id WHERE d.name is null and d.processing_start_time is not null " +
            "and d.processing_end_time is null and d.processing_start_time  < now() - (interval '12' hour)) " +
            "or t.id in (SELECT t3.id from tenant t3 join guarantor g on g.tenant_id = t3.id join document d " +
            "on d.guarantor_id = g.id WHERE d.name is null and d.processing_start_time is not null and d.processing_end_time is null " +
            "and d.processing_start_time  < now() - (interval '12' hour))", nativeQuery = true)
    long countAllTenantsWithFailedGeneratedPdfDocument();

    @Query(value = "SELECT * from tenant t join user_account u on t.id = u.id where t.id IN ((SELECT t2.id from tenant t2 join document d " +
            "on d.tenant_id = t2.id WHERE d.name is null and d.processing_start_time is not null " +
            "and d.processing_end_time is null and d.processing_start_time < now() - (interval '12' hour)) " +
            "union distinct (SELECT t3.id from tenant t3 join guarantor g on g.tenant_id = t3.id join document d " +
            "on d.guarantor_id = g.id WHERE d.name is null and d.processing_start_time is not null and d.processing_end_time is null " +
            "and d.processing_start_time < now() - (interval '12' hour)))", nativeQuery = true)
    Page<Tenant> findAllTenantsWithFailedGeneratedPdfDocument(Pageable pageable);

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

    @Query(value = "SELECT count(t.id) from tenant t " +
            "where " +
            "t.apartment_sharing_id =:apartmentSharingId " +
            "and (" +
            "  t.status != 'VALIDATED' " +
            "  or " +
            "  t.id in (SELECT t2.id from tenant t2 join document d on d.tenant_id = t2.id WHERE d.name is null) " +
            "  or " +
            "  t.id in (SELECT t2.id from tenant t2 join guarantor g on g.tenant_id = t2.id join document d on d.guarantor_id = g.id WHERE d.name is null)" +
            "    )", nativeQuery = true)
    int countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(@Param("apartmentSharingId") long apartmentSharingId);

    @Query(
            "select distinct t from Tenant t " +
                    " join Log l on t.id = l.tenantId " +
                    " where " +
                    " t.linkedKeycloakClients is null " +
                    " and " +
                    " t.id not in (select tu.tenant.id from TenantUserApi tu)" +
                    " and " +
                    " l.creationDateTime between :startDate and :endDate " +
                    " and " +
                    " l.logType = 'ACCOUNT_VALIDATED' "
    )
    List<Tenant> findAllTenantsNotAssociatedToPartnersAndValidatedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "select distinct t from Tenant t " +
                    " join Log l on t.id = l.tenantId " +
                    " where " +
                    " ( " +
                    "     t.linkedKeycloakClients is not null " +
                    "     or " +
                    "     t.id in (select tu.tenant.id from TenantUserApi tu)" +
                    " ) " +
                    " and " +
                    " l.creationDateTime between :startDate and :endDate " +
                    " and " +
                    " l.logType = 'ACCOUNT_VALIDATED' "
    )
    List<Tenant> findAllTenantsYESAssociatedToPartnersAndValidatedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

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
            SELECT t.id as id, t.apartment_sharing_id as apartmentSharingId, t.last_update_date as lastUpdateDate 
            FROM  tenant t
            INNER JOIN tenant_userapi tua ON tua.tenant_id = t.id  
            WHERE t.last_update_date >= :lastUpdateSince
            AND t.last_update_date < :lastUpdateBefore 
            AND tua.userapi_id = :partnerId 
            ORDER BY t.last_update_date
            """, nativeQuery = true
    )
    List<TenantUpdate> findTenantUpdateByLastUpdateIntervalAndPartner(@Param("lastUpdateSince") LocalDateTime lastUpdateSince, @Param("lastUpdateBefore") LocalDateTime lastUpdateBefore, @Param("partnerId") Long partnerId);
}
