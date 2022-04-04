package fr.dossierfacile.api.pdfgenerator.test.utils;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

abstract class HttpResponseAdapter implements HttpResponse {

    @Override
    public <T> T getEntity(Class<T> returnType) {
        return null;
    }

    @Override
    public <T> T getEntity(Class<T> returnType, ExecutionOptions<T> options) {
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> typeToReadAs) {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getStatusMessage() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public String header(String name) {
        return null;
    }

    @Override
    public Map<String, String> headers() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
