package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionedCategoriesMapper implements CategoriesMapper {

    @Override
    public DocumentCategory mapCategory(DocumentCategory category, UserApi userApi) {
        return category;
    }

    @Override
    public DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory, UserApi userApi) {
        return subCategory;
    }
}
