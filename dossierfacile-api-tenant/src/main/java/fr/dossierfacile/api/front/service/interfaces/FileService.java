package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;

public interface FileService {
    Document delete(Long fileId, Tenant tenant);
}
