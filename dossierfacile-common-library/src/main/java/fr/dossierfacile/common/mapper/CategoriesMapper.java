package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;

public interface CategoriesMapper {

    DocumentCategory mapCategory(DocumentCategory category);

    DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory);

}
