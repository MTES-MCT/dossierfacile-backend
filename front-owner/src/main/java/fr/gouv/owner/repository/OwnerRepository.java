package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {
    Owner findOneByEmail(String email);

    @Query(value = "SELECT count(*), month(creation_date) as month, year(creation_date)  as year\n" +
            "FROM owner JOIN user_account ON owner.id = user_account.id GROUP BY month, year ORDER BY  year desc, month desc", nativeQuery = true)
    List<Object> accountsByMonth();

    Owner findBySlug(String slug);

    @Query("select count(u.id) from User u join Owner o on u.id = u.id")
    Long countOwners();
}
