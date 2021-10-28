package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByEmailAndEnabledFalse(String email);

    @Query("from Tenant t where t.email in :emails")
    List<Tenant> findByListEmail(@Param("emails") List<String> emails);

    List<Tenant> findAllByApartmentSharing(ApartmentSharing apartmentSharing);

    Optional<Tenant> findByEmail(String name);

    List<Tenant> findAllByEnabledIsFalseAndCreationDateTimeIsBetween(LocalDateTime initDate, LocalDateTime endDate);

    @Query("from Tenant t " +
            "join Log l on t.id = l.tenantId " +
            "where t.honorDeclaration = false and l.logType = 'EMAIL_ACCOUNT_VALIDATED' and l.creationDateTime between :initDate and :endDate")
    List<Tenant> findAllByHonorDeclarationIsFalseAndCompletionDateTimeIsBetween(@Param("initDate") LocalDateTime initDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "select distinct t2 from Tenant t2 " +
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
}
