package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;

@Component
public class DummyCategoriesMapper implements CategoriesMapper {

    @Override
    public DocumentCategory mapCategory(DocumentCategory category) {
        return category;
    }

    @Override
    public DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory) {
        return subCategory;
    }

}
