package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves the resources that the dossier HTML references, from the classpath and from memory:
 * <ul>
 *     <li>{@code res:path} is a static file of {@code templates/dossier-pdf/}, such as the stylesheet or an icon;</li>
 *     <li>{@code mem:name} is a file generated for this dossier only, such as a piece page or the QR code.</li>
 * </ul>
 */
@Slf4j
class DossierPdfResources implements FSStreamFactory {

    static final String STATIC_PROTOCOL = "res";
    static final String MEMORY_PROTOCOL = "mem";
    private static final String STATIC_ROOT = "templates/dossier-pdf/";

    private final Map<String, byte[]> memory = new HashMap<>();

    /** Keeps the bytes in memory and returns the URI that references them in the HTML. */
    String add(String name, byte[] bytes) {
        String uri = MEMORY_PROTOCOL + ":" + name;
        memory.put(uri, bytes);
        return uri;
    }

    @Override
    public FSStream getUrl(String url) {
        byte[] bytes = url.startsWith(MEMORY_PROTOCOL + ":") ? memory.get(url) : readStatic(url);
        if (bytes == null) {
            log.warn("Dossier PDF resource not found: {}", url);
        }
        return new FSStream() {
            @Override
            public InputStream getStream() {
                return bytes == null ? null : new ByteArrayInputStream(bytes);
            }

            @Override
            public Reader getReader() {
                return bytes == null ? null : new InputStreamReader(getStream(), StandardCharsets.UTF_8);
            }
        };
    }

    private byte[] readStatic(String url) {
        String path = STATIC_ROOT + url.substring(STATIC_PROTOCOL.length() + 1);
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
