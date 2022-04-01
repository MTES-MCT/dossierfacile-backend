package fr.dossierfacile.api.pdfgenerator.test.utils;

import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.storage.block.options.DownloadOptions;
import org.openstack4j.model.storage.object.SwiftObject;

import java.util.Date;
import java.util.Map;

abstract class SwiftObjectAdapter implements SwiftObject {
    @Override
    public String getETag() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public long getSizeInBytes() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDirectoryName() {
        return null;
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public String getContainerName() {
        return null;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public Map<String, String> getMetadata() {
        return null;
    }

    @Override
    public DLPayload download() {
        return null;
    }

    @Override
    public DLPayload download(DownloadOptions options) {
        return null;
    }
}
