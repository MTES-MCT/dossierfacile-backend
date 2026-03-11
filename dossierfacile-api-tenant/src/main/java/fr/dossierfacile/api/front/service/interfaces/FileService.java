package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;

public interface FileService {
    Document delete(Long fileId, Tenant tenant);

    File getFileForTenantOrCouple(Long fileId, Tenant tenant) throws FileNotFoundException;
}
