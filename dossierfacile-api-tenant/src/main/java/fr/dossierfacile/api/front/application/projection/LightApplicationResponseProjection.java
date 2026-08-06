package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.springframework.stereotype.Component;

/**
 * Light view: no document names, no dossier links. Everything else is identical to full.
 */
@Component
public class LightApplicationResponseProjection extends SharedApplicationResponseProjection {

    public ApplicationModel project(ApplicationProjectionSources sources) {
        return assemble(sources, document -> null);
    }
}
