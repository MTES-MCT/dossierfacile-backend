package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentDeniedOptionsRepository extends JpaRepository<DocumentDeniedOptions, Integer> {

    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserTypeOrderByCode(DocumentSubCategory documentSubCategory, String documentUserType);

    List<DocumentDeniedOptions> findAllByDocumentSubCategory(DocumentSubCategory documentSubCategory);

    @Query(value = """
            select *
            from document_denied_options
            where (document_user_type = 'all' or document_user_type = :documentUserType)
              and (document_sub_category = 'UNDEFINED' or document_sub_category = :documentSubCategory)
            ORDER BY
                CASE
                WHEN document_sub_category = 'UNDEFINED' THEN 1
                ELSE 0
            END, code
            """, nativeQuery = true)
    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserTypeIncludeGeneric(String documentSubCategory, String documentUserType);

}
