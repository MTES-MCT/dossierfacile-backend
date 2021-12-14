package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.owner.dto.CountDTO;
import fr.gouv.owner.dto.SuperFacileDTO;
import fr.gouv.owner.dto.TenantBOIndexDTO;
import fr.gouv.owner.projection.TenantPrincipalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {


    Tenant findOneByEmail(String email);

    List<Tenant> findTenantByApartmentSharingId(Long id);

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

    @Query(value = "select t.id as id, ua.creation_date as creationDateTime, ua.first_name as firstName, ua.last_name as lastName, ua.email as email, t.user_api_id as userApiId, t.partner_id as partnerId\n" +
            "from tenant t\n" +
            "         join user_account ua on t.id = ua.id\n" +
            "         join apartment_sharing ap on t.apartment_sharing_id = ap.id\n" +
            "where tenant_file_status = 0" +
            "  and (ap.operator_date is null or ap.operator_date < :localDateTime) order by creationDateTime", nativeQuery = true)
    Page<TenantBOIndexDTO> findByTenantFileStatus(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);

    @Transactional
    @Modifying
    void deleteByUpdateDateTimeBeforeAndLastLoginDateBefore(Date toDate, LocalDateTime fourWeeks);

    @Transactional
    @Modifying
    @Query("DELETE FROM Tenant t WHERE t.id = :userId")
    void deleteTenantSharingApartment(@Param("userId") Integer userId);

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

    @Query(value = "SELECT count(*) from tenant JOIN user_account  ON tenant.id = user_account.id", nativeQuery = true)
    long countAllTenantAccount();

    @Query(value = "select l.creation_date , t.satisfaction_survey from log l JOIN tenant t ON  l.tenant_id=t.id JOIN user_account u ON t.id=u.id and l.log_type=5 and u.last_login_date>l.creation_date", nativeQuery = true)
    List<Object> tenantsSatisfactionStatistics();

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

    @Modifying
    @Query(value = "update tenant t set apartment_sharing_id = null from user_account u where u.id = t.id", nativeQuery = true)
    void deletedTenantsSetNullApartmentSharing();

    @Query(value = "select t.id,ua.email,t.user_api_id as userApiId from tenant t join user_account ua on t.id = ua.id where t.tenant_type='CREATE' and t.apartment_sharing_id=:id", nativeQuery = true)
    TenantPrincipalDTO findPrincipalTenant(@Param("id") int id);

    List<Tenant> findAllByApartmentSharingId(int ap);

    Tenant findOneByApartmentSharingAndTenantType(ApartmentSharing apartmentSharing, TenantType create);

    Tenant findOneByApartmentSharingIdAndTenantType(int id, TenantType create);

    Tenant findOneById(Integer id);

    @Query(value = "select *\n" +
            "from tenant\n" +
            "where extract(year from update_by_tenant) <= :year and\n" +
            "      extract(month from update_by_tenant) <= :month", nativeQuery = true)
    List<Tenant> findByUpdateByTenant(@Param("year") int year, @Param("month") int month);
}
