package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;

@Component
public class DummySubCategoryMapper implements SubCategoryMapper {

    @Override
    public DocumentSubCategory map(DocumentSubCategory subCategory) {
        return subCategory;
    }

}
