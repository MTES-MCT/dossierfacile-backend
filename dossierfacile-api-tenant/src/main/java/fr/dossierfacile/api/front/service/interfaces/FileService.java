package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;

public interface FileService {
    Document delete(Long fileId);
}
