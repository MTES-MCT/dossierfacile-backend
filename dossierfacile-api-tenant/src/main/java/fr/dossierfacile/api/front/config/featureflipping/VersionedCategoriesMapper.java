package fr.dossierfacile.api.front.config.featureflipping;

import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.mapper.CategoriesMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentSubCategory.CERTIFICATE_VISA;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_IDENTIFICATION;

@Primary
@Component
@AllArgsConstructor
public class VersionedCategoriesMapper implements CategoriesMapper {

    private final ClientAuthenticationFacade clientAuthenticationFacade;

    @Override
    public DocumentCategory mapCategory(DocumentCategory category) {
        Optional<Integer> version = clientAuthenticationFacade.getApiVersion();
        if (version.isPresent() && version.get() < 3) {
            return hideCategoriesIntroducedInV3(category);
        }
        return category;
    }

    @Override
    public DocumentSubCategory mapSubCategory(DocumentSubCategory subCategory) {
        Optional<Integer> version = clientAuthenticationFacade.getApiVersion();
        if (version.isPresent() && version.get() < 3) {
            return hideSubCategoriesIntroducedInV3(subCategory);
        }
        return subCategory;
    }

    private DocumentCategory hideCategoriesIntroducedInV3(DocumentCategory category) {
        if (category == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE) {
            return DocumentCategory.IDENTIFICATION;
        }
        return category;
    }

    private DocumentSubCategory hideSubCategoriesIntroducedInV3(DocumentSubCategory subCategory) {
        return switch (subCategory) {
            case FRANCE_IDENTITE -> OTHER_IDENTIFICATION;
            case VISALE, OTHER_GUARANTEE -> CERTIFICATE_VISA;
            default -> subCategory;
        };
    }

}
