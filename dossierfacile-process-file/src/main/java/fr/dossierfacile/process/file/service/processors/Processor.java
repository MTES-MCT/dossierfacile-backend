package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.process.file.service.AnalysisContext;

public interface Processor {
    AnalysisContext process(AnalysisContext context);
    /**
     * (Optional) Clean up the elements that were added to the context by this process.
     */
    default AnalysisContext cleanContext(AnalysisContext context) {
        return context;
    }
}
