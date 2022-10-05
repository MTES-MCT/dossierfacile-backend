package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentDeniedOptionsRepository extends JpaRepository<DocumentDeniedOptions, Integer> {

    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserTypeOrderByCode(DocumentSubCategory documentSubCategory, String documentUserType);

    List<DocumentDeniedOptions> findAllByDocumentSubCategory(DocumentSubCategory documentSubCategory);

    DocumentDeniedOptions findByCode(String code);

}
