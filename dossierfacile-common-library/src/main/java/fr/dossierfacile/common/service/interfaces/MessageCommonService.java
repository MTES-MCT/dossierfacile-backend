package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.User;

public interface MessageCommonService {
    void markReadAdmin(User tenant);
}
