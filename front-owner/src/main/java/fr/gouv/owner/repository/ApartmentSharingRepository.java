package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.gouv.owner.projection.ApartmentSharingDTO01;
import fr.gouv.owner.projection.ApartmentSharingDTO02;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentSharingRepository extends JpaRepository<ApartmentSharing, Integer> {

    ApartmentSharing findOneByToken(String token);

    ApartmentSharing findOneByTokenPublic(String token);

    @Query(value = "select a.id as id,\n" +
            "       t.user_api_id as userApiId,\n" +
            "       t.partner_id as partnerId,\n" +
            "       a.creation_date as creationDate,\n" +
            "       u.first_name as firstName,\n" +
            "       u.last_name as lastName,\n" +
            "       u.deletion_date as deletionDate,\n" +
            "       (select count(*) from tenant t join user_account ua on t.id = ua.id where ua.deletion_date is null and t.apartment_sharing_id = a.id and ua.deletion_date is null) as numberOfTenants,\n" +
            "       (select count(*)\n" +
            "        from tenant t join user_account ua2 on t.id = ua2.id\n" +
            "        where ua2.deletion_date is null and t.apartment_sharing_id = a.id\n" +
            "          and t.step is null)                  as numberOfCompleteRegister,\n" +
            "       (select coalesce(\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua3 on t.id = ua3.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 3\n" +
            "                          and ua3.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join  user_account ua4 on t.id = ua4.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 0\n" +
            "                          and ua4.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua5 on t.id = ua5.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 2\n" +
            "                          and ua5.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua6 on t.id = ua6.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 1\n" +
            "                          and ua6.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       0\n" +
            "                   ))                                                      as ordinalStatus\n" +
            "from apartment_sharing a\n" +
            "         join tenant t on a.id = t.apartment_sharing_id\n" +
            "         join user_account u on t.id = u.id\n" +
            "where t.tenant_type = 'CREATE' and u.deletion_date is null",
            countQuery = "select count(*) \n" +
                    "from apartment_sharing a\n" +
                    "         join tenant t on a.id = t.apartment_sharing_id\n" +
                    "         join user_account u on t.id = u.id\n" +
                    "where t.tenant_type = 'CREATE' and u.deletion_date is null",
            nativeQuery = true)
    Page<ApartmentSharingDTO01> findAllByOrderByIdDesc(Pageable pageable);

    @Query("select apt from ApartmentSharing apt where apt.id in (:ids)")
    List<ApartmentSharing> findIn(@Param("ids") List<Integer> ids);

    @Query(value = "select coalesce(\n" +
            "               (select t.tenant_file_status\n" +
            "                from tenant t join user_account ua on t.id = ua.id\n" +
            "                where apartment_sharing_id = :apartmentSharing\n" +
            "                  and tenant_file_status = 3\n" +
            "                  and deletion_date is null\n" +
            "                fetch first 1 row only),\n" +
            "               (select t.tenant_file_status\n" +
            "                from tenant t join user_account u on t.id = u.id\n" +
            "                where apartment_sharing_id = :apartmentSharing\n" +
            "                  and tenant_file_status = 0\n" +
            "                  and deletion_date is null\n" +
            "                fetch first 1 row only),\n" +
            "               (select t.tenant_file_status\n" +
            "                from tenant t join user_account a on t.id = a.id\n" +
            "                where apartment_sharing_id = :apartmentSharing\n" +
            "                  and tenant_file_status = 2\n" +
            "                  and deletion_date is null\n" +
            "                fetch first 1 row only),\n" +
            "               (select t.tenant_file_status\n" +
            "                from tenant t join user_account ua2 on t.id = ua2.id\n" +
            "                where apartment_sharing_id = :apartmentSharing\n" +
            "                  and tenant_file_status = 1\n" +
            "                  and deletion_date is null\n" +
            "                fetch first 1 row only),\n" +
            "               0\n" +
            "           ) as status", nativeQuery = true)
    int getStatus(@Param("apartmentSharing") ApartmentSharing apartmentSharing);

    @Query(value = "select sum(salary) from tenant t join user_account ua on t.id = ua.id where apartment_sharing_id=:apartmentSharing and deletion_date is null", nativeQuery = true)
    Integer totalSalary(@Param("apartmentSharing") ApartmentSharing apartmentSharing);

    @Query(value = "select count (t) from tenant t join user_account ua on t.id = ua.id where apartment_sharing_id=:apartmentSharing and step is not null and deletion_date is null", nativeQuery = true)
    int completeRegister(@Param("apartmentSharing") ApartmentSharing apartmentSharing);

    @Query(value = "select a.id,\n" +
            "       (select count(*) from tenant t join user_account ua on t.id = ua.id where ua.deletion_date is null and t.apartment_sharing_id = a.id and ua.deletion_date is null) as numberOfTenants,\n" +
            "       (select count(*) from tenant t join user_account u on t.id = u.id where apartment_sharing_id = a.id and step is null and a.deletion_date is null) as numberOfCompleteRegister,\n" +
            "       (select coalesce(\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua2 on t.id = ua2.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 3\n" +
            "                          and ua2.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua3 on t.id = ua3.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 0\n" +
            "                          and ua3.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua4 on t.id = ua4.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and tenant_file_status = 2\n" +
            "                          and ua4.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       (select t.tenant_file_status\n" +
            "                        from tenant t join user_account ua5 on t.id = ua5.id\n" +
            "                        where apartment_sharing_id = a.id\n" +
            "                          and ua5.deletion_date is null\n" +
            "                        fetch first 1 row only),\n" +
            "                       0\n" +
            "                   ))                                                                      as status,\n" +
            "       (select sum(t.salary)\n" +
            "        from tenant t join user_account ua6 on t.id = ua6.id\n" +
            "        where t.apartment_sharing_id = a.id and ua6.deletion_date is null)                 as totalSalaryTenant,\n" +
            "       a.token                                                                             as token,\n" +
            "       a.token_public                                                                      as tokenPublic\n" +
            "from apartment_sharing a\n" +
            "where a.id = :id", nativeQuery = true)
    ApartmentSharingDTO02 apartmentSharing(@Param("id") int id);

    @Query(value = "select a.* from apartment_sharing a join tenant t on a.id = t.apartment_sharing_id where t.id=:id", nativeQuery = true)
    ApartmentSharing findByTenant(@Param("id") Integer id);
}
