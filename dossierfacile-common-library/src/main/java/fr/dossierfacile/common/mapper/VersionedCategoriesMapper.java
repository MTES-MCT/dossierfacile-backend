package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VersionedCategoriesMapper implements CategoriesMapper {
    @Value("${application.api.version}")
    private Integer applicationApiVersion;

    @Override
    public DocumentCategory mapCategory(DocumentCategory category, UserApi userApi) {
        return category;
    }

    @Override
    public DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory, UserApi userApi) {
        if (userApi != null && applicationApiVersion != null
                && !Objects.equals(userApi.getVersion(), applicationApiVersion)
                && applicationApiVersion == 4 && userApi.getVersion() == 3
                && subCategory == DocumentSubCategory.GUEST_COMPANY) {
            return DocumentSubCategory.GUEST_ORGANISM;
        }
        return subCategory;
    }
}