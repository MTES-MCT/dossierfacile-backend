package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLogger;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Level;

/**
 * Sends the logs of openhtmltopdf to SLF4J. By default, the engine writes them to the console only, so they miss
 * our log files. This logger keeps the progress messages of the engine at debug level.
 */
@Slf4j
class HtmlEngineLogger implements XRLogger {

    private static final String FORMAT = "{}: {}";

    @Override
    public void log(Diagnostic diagnostic) {
        // The decorative images of our templates have an empty alternative on purpose.
        boolean decorativeImage = diagnostic.getLogMessageId() == LogMessageId.LogMessageId0Param.GENERAL_PDF_ACCESSIBILITY_NO_ALT_ATTRIBUTE_PROVIDED_FOR_IMAGE;
        log(diagnostic.getLogMessageId().getWhere(), decorativeImage ? Level.FINE : diagnostic.getLevel(),
                diagnostic.getFormattedMessage(), diagnostic.getError());
    }

    @Override
    public void log(String where, Level level, String message) {
        log(where, level, message, null);
    }

    @Override
    public void log(String where, Level level, String message, Throwable error) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            log.error(FORMAT, where, message, error);
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            log.warn(FORMAT, where, message, error);
        } else {
            log.debug(FORMAT, where, message, error);
        }
    }

    @Override
    public void setLevel(String where, Level level) {
        // The SLF4J configuration sets the levels.
    }

    @Override
    public boolean isLogLevelEnabled(Diagnostic diagnostic) {
        return log.isDebugEnabled() || diagnostic.getLevel().intValue() >= Level.WARNING.intValue();
    }
}
