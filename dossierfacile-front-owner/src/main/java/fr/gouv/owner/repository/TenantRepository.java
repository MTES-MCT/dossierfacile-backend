package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.owner.dto.CountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Tenant findOneByEmail(String email);

    List<Tenant> findTenantByApartmentSharingId(Long id);

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year \n" +
            "FROM tenant JOIN user_account ON tenant.id = user_account.id \n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countAllRegisteredTenant();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload1 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload1 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload1IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload2 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload2 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload2IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload3 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload3 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload3IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload4 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload4 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload4IsNotNullTenantGuarantor();

    @Query(value = "select count(*), extract (week from creation_date) as week, extract (year from creation_date) as year\n" +
            "From\n" +
            "(select creation_date FROM tenant JOIN user_account ON tenant.id = user_account.id where upload5 IS NOT NULL\n" +
            "UNION ALL\n" +
            " select creation_date FROM guarantor where upload5 IS NOT NULL\n" +
            ") as tenat_guarantor\n" +
            "GROUP BY week,year ORDER BY year desc, week desc", nativeQuery = true)
    List<CountDTO> countByUpload5IsNotNullTenantGuarantor();

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

    Tenant findOneByApartmentSharingAndTenantType(ApartmentSharing apartmentSharing, TenantType create);

    Tenant findOneById(Long id);

}
