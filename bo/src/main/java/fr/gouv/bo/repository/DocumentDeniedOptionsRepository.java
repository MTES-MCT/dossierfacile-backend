package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentDeniedOptionsRepository extends JpaRepository<DocumentDeniedOptions, Integer> {

    List<DocumentDeniedOptions> findAllByDocumentSubCategoryAndDocumentUserType(DocumentSubCategory documentSubCategory, String documentUserType);

    @Query(value ="select d.* from document_denied_options d where d.document_sub_category = :documentSubCategory and d.document_user_type = :userType and d.message_value = :message",nativeQuery = true)
    DocumentDeniedOptions findOneDocumentDeniedOptions(@Param("documentSubCategory")String documentSubCategory, @Param("userType")String userType, @Param("message") String message);

}
