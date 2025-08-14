package fr.dossierfacile.process.file.service.documentrules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import org.springframework.lang.Nullable;

public class BlurryRuleHelper {

    private BlurryRuleHelper () {
        // Utility class, no instantiation needed
    }

    public static boolean isBlank(@Nullable BlurryFileAnalysis blurryFileAnalysis) {
        if (blurryFileAnalysis == null) {
            return false; // If the analysis is null, we consider it as blank
        }
        return blurryFileAnalysis.getBlurryResults().isBlank();
    }
}
