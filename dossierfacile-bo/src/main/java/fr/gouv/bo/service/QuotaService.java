package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Quota;
import fr.gouv.bo.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaService {

    final private QuotaRepository quotaRepository;

    private Quota getMatchingQuota(String email, String endpointPath) {

        AntPathMatcher matcher = new AntPathMatcher();
        return quotaRepository
                .findAllByEmail(email)
                .stream().filter(
                        q -> matcher.match(q.getEndpointPath(), endpointPath)
                ).findFirst().orElse(null);
    }

    public boolean checkQuota(String email, String endpointPath) {

        Quota quota = getMatchingQuota(email, endpointPath);
        if (quota == null) {
            quota = getMatchingQuota("default", endpointPath);
            if (quota != null) {
                quota.setId(null);
                quota.setEmail(email);
            } else {
                return true;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (!quota.getCurrentDay().isEqual(now.toLocalDate())) {
            quota.setCurrentDay(now.toLocalDate());
            quota.setUsedDailyRequests(0);
            quotaRepository.save(quota);
        }
        if (quota.getUsedDailyRequests() < quota.getMaxDailyRequests()) {
            quota.setUsedDailyRequests(quota.getUsedDailyRequests() + 1);
            quotaRepository.save(quota);
            return true;
        }
        return false;
    }
}
