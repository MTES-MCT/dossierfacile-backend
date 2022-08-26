package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentDeniedOptionsRepository extends JpaRepository<DocumentDeniedOptions, Integer> {

    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserType(DocumentSubCategory documentSubCategory, String documentUserType);

}
