package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@AllArgsConstructor
public class TenantLogCommonServiceImpl implements TenantLogCommonService {

    private final TenantLogRepository tenantLogRepository;
    private final LotteryTicketRepository lotteryTicketRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveTenantLog(TenantLog log) {
        tenantLogRepository.save(log);
    }

    @Override
    public void logQueueEntered(Long tenantId, QueueEntrySource source) {
        // TODO(lottery-bypass): drop the bypass flag (and the LotteryTicketRepository
        // dependency) once every dossier goes through the lottery
        boolean drawnFromLottery = lotteryTicketRepository
                .findFirstByTenantIdAndStatusIn(tenantId, Set.of(LotteryTicketStatus.DRAWN))
                .isPresent();
        ObjectNode details = objectMapper.createObjectNode();
        details.put("source", source.name());
        details.put("bypass", !drawnFromLottery);
        tenantLogRepository.save(TenantLog.builder()
                .logType(LogType.QUEUE_ENTERED)
                .tenantId(tenantId)
                .logDetails(details)
                .build());
    }

}
