package fr.dossierfacile.api.front.application.projection;

import org.springframework.stereotype.Component;

/**
 * Builds the light-variant read view: no document names, no dossier links.
 * Everything else is identical to full — same projection hierarchy downstream.
 */
@Component
public class LightApplicationViewAssembler {

    public ApplicationReadView assemble(ApplicationProjectionSources sources) {
        return ApplicationReadView.from(sources, document -> null, null, null);
    }
}
