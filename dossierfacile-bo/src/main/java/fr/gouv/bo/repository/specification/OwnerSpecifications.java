package fr.gouv.bo.repository.specification;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class OwnerSpecifications {

    private OwnerSpecifications() {
    }

    public static Specification<Owner> emailContains(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return likeOnUserAccount("email", email);
    }

    public static Specification<Owner> firstNameContains(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            return null;
        }
        return likeOnUserAccount("firstName", firstName);
    }

    public static Specification<Owner> lastNameContains(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return null;
        }
        return likeOnUserAccount("lastName", lastName);
    }

    private static Specification<Owner> likeOnUserAccount(String fieldName, String value) {
        final String normalizedPattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";

        return (root, query, criteriaBuilder) -> {
            // Owner inherits User (user_account table), so we correlate by id to filter on user fields.
            Subquery<Long> userAccountSubquery = query.subquery(Long.class);
            Root<User> userAccountRoot = userAccountSubquery.from(User.class);
            userAccountSubquery
                    .select(userAccountRoot.get("id"))
                    .where(
                            criteriaBuilder.equal(userAccountRoot.get("id"), root.get("id")),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(userAccountRoot.get(fieldName)),
                                    normalizedPattern
                            )
                    );
            return criteriaBuilder.exists(userAccountSubquery);
        };
    }
}

