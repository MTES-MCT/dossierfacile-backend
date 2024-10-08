package fr.dossierfacile.api.dossierfacileapiowner.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.OwnerLog;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.mapper.log.DeletedOwnerMapper;
import fr.dossierfacile.common.utils.MapperUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class OwnerLogServiceImpl implements OwnerLogService {
    private final OwnerLogRepository repository;
    private final DeletedOwnerMapper deletedOwnerMapper;
    private final ObjectMapper objectMapper = MapperUtil.newObjectMapper();

    private void saveLog(OwnerLog log) {
        repository.save(log);
    }

    @Override
    public void saveLog(OwnerLogType logType, Long ownerId) {
        this.saveLog(OwnerLog.builder().logType(logType).creationDateTime(LocalDateTime.now()).ownerId(ownerId).build());
    }

    @Override
    public void saveLogWithOwnerData(OwnerLogType logType, Owner owner) {
        String content = null;
        try {
            content = objectMapper.writeValueAsString(deletedOwnerMapper.toDeletedOwnerModel(owner));
        } catch (Exception e) {
            log.error("Cannot correclty record tenant information in tenant_log");
        }
        this.saveLog(
                OwnerLog.builder()
                        .logType(logType)
                        .ownerId(owner.getId())
                        .creationDateTime(LocalDateTime.now())
                        .jsonProfile(content)
                        .build()
        );
    }

}
