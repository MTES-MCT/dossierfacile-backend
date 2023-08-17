package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class LinkLogServiceImpl implements LinkLogService {
    private final LinkLogRepository linkLogRepository;

    public LinkLog save(LinkLog log) {
        return linkLogRepository.save(log);
    }
}
