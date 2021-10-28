package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.gouv.bo.dto.CountDTO;
import fr.gouv.bo.dto.SuperFacileDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Tenant findOneByEmail(String email);

    List<Tenant> findByLastLoginDateBetween(LocalDateTime startLogin, LocalDateTime endLogin);

    @Query("select distinct t FROM Tenant t where concat(LOWER(t.firstName),' ',LOWER(t.lastName)) like CONCAT('%',:nameUser,'%') ")
    List<Tenant> findTenantByFirstNameOrLastNameOrFullName(@Param("nameUser") String nameUser);

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countAllRegisteredTenant();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByCreatedTenant();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id where upload1 IS NOT NULL\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload1IsNotNull();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload1 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload1 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload1IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id where upload2 IS NOT NULL \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload2IsNotNull();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload2 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload2 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload2IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id where upload3 IS NOT NULL \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload3IsNotNull();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload3 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload3 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload3IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id where upload4 IS NOT NULL \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload4IsNotNull();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload4 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload4 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload4IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id where upload5 IS NOT NULL \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload5IsNotNull();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload5 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload5 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload5IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id\n" +
            "where\n" +
            "  upload1 is null and\n" +
            "  upload2 is null and\n" +
            "  upload3 is null and\n" +
            "  upload4 is null and\n" +
            "  upload5 is null\n" +
            "GROUP BY week,year ORDER BY year desc, week desc;", nativeQuery = true)
    List<CountDTO> countByFilesNotUploaded();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where \n" +
            "  upload1 is null and\n" +
            "  upload2 is null and\n" +
            "  upload3 is null and\n" +
            "  upload4 is null and\n" +
            "  upload5 is null\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where \n" +
            "  upload1 is null and\n" +
            "  upload2 is null and\n" +
            "  upload3 is null and\n" +
            "  upload4 is null and\n" +
            "  upload5 is null\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByFilesNotUploadedTenantGuarantor();

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

    @Query(value = "SELECT count(*) from tenant JOIN user_account  ON tenant.id = user_account.id", nativeQuery = true)
    long countAllTenantAccount();

    @Query(value = "select " +
            "       sum(case when step_super_facile_major = 4 then 1 else 0 end) as complete,\n" +
            "       sum(case when step_super_facile_major = 3 or step_super_facile_major = 4 then 1 else 0 end) as email,\n" +
            "       sum(case when step_super_facile_major = 1 or step_super_facile_major = 2 or step_super_facile_major = 3 or step_super_facile_major = 4 then 1 else 0 end) as info,\n" +
            "       sum(case when step_super_facile_major = 2 or step_super_facile_major = 3 or step_super_facile_major = 4 then 1 else 0 end) as name,\n" +
            "       count(*)                                               as total,\n" +
            "       extract(week from creation_date)                       as week,\n" +
            "       extract(year from creation_date)                       as year\n" +
            "FROM tenant\n" +
            "       JOIN user_account ON tenant.id = user_account.id\n" +
            "GROUP BY week, year\n" +
            "ORDER BY year desc, week desc", nativeQuery = true)
    List<SuperFacileDTO> dsf();

    List<Tenant> findAllByApartmentSharingId(Long ap);

    Tenant findOneById(Long id);

    List<Tenant> findAllByApartmentSharing(ApartmentSharing apartmentSharing);

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
            "and d.processing_end_time is null and d.processing_start_time  < now() - (interval '1' hour)) " +
            "or t.id in (SELECT t3.id from tenant t3 join guarantor g on g.tenant_id = t3.id join document d " +
            "on d.guarantor_id = g.id WHERE d.name is null and d.processing_start_time is not null and d.processing_end_time is null " +
            "and d.processing_start_time  < now() - (interval '1' hour))", nativeQuery = true)
    long countAllTenantsWithFailedGeneratedPdfDocument();

    List<Tenant> getTenantsByStatus(TenantFileStatus tenantFileStatus);
}
