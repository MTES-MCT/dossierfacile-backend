package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.UserFeatureAssignment;
import fr.dossierfacile.common.entity.UserFeatureAssignmentHistory;
import fr.dossierfacile.common.repository.UserFeatureAssignmentHistoryRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentRepository;
import fr.dossierfacile.common.service.interfaces.UserFeatureAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFeatureAssignmentServiceImpl implements UserFeatureAssignmentService {

    private final UserFeatureAssignmentRepository featureAssignmentRepository;
    private final UserFeatureAssignmentHistoryRepository featureAssignmentHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveAssignment(UserFeatureAssignment assignment, UserFeatureAssignmentHistory history, boolean fallbackEnabled) {
        try {
            featureAssignmentRepository.saveAndFlush(assignment);
            featureAssignmentHistoryRepository.save(history);
            return fallbackEnabled;
        } catch (DataIntegrityViolationException e) {
            return featureAssignmentRepository.findById(assignment.getId())
                    .map(UserFeatureAssignment::isEnabled)
                    .orElse(fallbackEnabled);
        }
    }
}

