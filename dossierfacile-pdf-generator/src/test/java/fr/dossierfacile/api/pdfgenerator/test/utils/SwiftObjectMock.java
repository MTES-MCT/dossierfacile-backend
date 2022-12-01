package fr.dossierfacile.api.pdfgenerator.test.utils;

import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.common.DLPayload;

import java.io.File;
import java.io.InputStream;

public class SwiftObjectMock extends SwiftObjectAdapter {
    String name;
    String filePath;

    public SwiftObjectMock(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DLPayload download() {
        return new DLPayload() {
            @Override
            public HttpResponse getHttpResponse() {
                return new HttpResponseAdapter() {
                    @Override
                    public int getStatus() {
                        return HttpStatus.SC_OK;
                    }

                };
            }

            @SneakyThrows
            @Override
            public InputStream getInputStream() {
                return SwiftObjectMock.class.getResourceAsStream(filePath);
            }

            @Override
            public void writeToFile(File file) {
            }
        };
    }
}
