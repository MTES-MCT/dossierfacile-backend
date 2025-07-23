package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentDeniedOptionsRepository extends JpaRepository<DocumentDeniedOptions, Integer> {

    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserTypeOrderByCode(DocumentSubCategory documentSubCategory, String documentUserType);

    @Query(value = """
                SELECT *
                FROM document_denied_options
                WHERE
                    (:documentCategory IS NULL OR document_category = :documentCategory)
                    AND
                    (:documentSubCategory IS NULL OR document_sub_category = :documentSubCategory)
            """, nativeQuery = true)
    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentCategory(String documentCategory, String documentSubCategory);

    List<DocumentDeniedOptions> findAllByDocumentSubCategory(DocumentSubCategory documentSubCategory);

    List<DocumentDeniedOptions> findAllByDocumentCategory(DocumentCategory documentCategory);

    @Query(value = """
            select * from document_denied_options
            where (document_sub_category = :documentSubCategory and document_user_type = :documentUserType)
                or (document_sub_category = :documentSubCategory and document_user_type = 'all')
                or (document_category = :documentCategory and document_sub_category = 'UNDEFINED' and document_user_type = :documentUserType)
                or (document_category = :documentCategory and document_sub_category = 'UNDEFINED' and document_user_type = 'all')
                or (document_category = 'NULL' and document_sub_category = 'UNDEFINED' and  document_user_type = :documentUserType)
                or (document_category = 'NULL' and document_sub_category = 'UNDEFINED' and document_user_type = 'all')
            order by
                case
                    when document_sub_category = :documentSubCategory and document_user_type = :documentUserType then 1
                    when document_sub_category = :documentSubCategory and document_user_type = 'all' then 2
                    when document_category = :documentCategory and document_sub_category = 'UNDEFINED' and document_user_type = :documentUserType then 3
                    when document_category = :documentCategory and document_sub_category = 'UNDEFINED' and document_user_type = 'all' then 4
                    when document_category = 'NULL' and document_sub_category = 'UNDEFINED' and document_user_type = :documentUserType then 5
                    when document_category = 'NULL' and document_sub_category = 'UNDEFINED' and document_user_type = 'all' then 6
                    else 7
            end,
            code;
            """, nativeQuery = true)
    List<DocumentDeniedOptions> findAllByDocumentCategoryAndDocumentSubCategoryAndDocumentUserTypeIncludeGeneric(String documentCategory, String documentSubCategory, String documentUserType);

}
