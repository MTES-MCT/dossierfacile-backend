package fr.dossierfacile.api.front.config.featureflipping;

import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.mapper.SubCategoryMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentSubCategory.CERTIFICATE_VISA;
import static fr.dossierfacile.common.enums.DocumentSubCategory.FRANCE_IDENTITE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_IDENTIFICATION;

@Primary
@Component
@AllArgsConstructor
public class VersionedSubCategoryMapper implements SubCategoryMapper {

    private final ClientAuthenticationFacade clientAuthenticationFacade;

    @Override
    public DocumentSubCategory map(DocumentSubCategory subCategory) {
        Optional<Integer> version = clientAuthenticationFacade.getApiVersion();
        if (version.isPresent() && version.get() < 3) {
            return hideSubCategoriesIntroducedInV3(subCategory);
        }
        return subCategory;
    }

    private DocumentSubCategory hideSubCategoriesIntroducedInV3(DocumentSubCategory subCategory) {
        return switch (subCategory) {
            case FRANCE_IDENTITE -> OTHER_IDENTIFICATION;
            case VISALE, OTHER_GUARANTEE -> CERTIFICATE_VISA;
            default -> subCategory;
        };
    }

}
