package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;

public interface CategoriesMapper {

    DocumentCategory mapCategory(DocumentCategory category, UserApi userApi);

    DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory, UserApi userApi);

}
