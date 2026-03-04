package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.UserFeatureAssignment;
import fr.dossierfacile.common.entity.UserFeatureAssignmentHistory;

public interface UserFeatureAssignmentService {
    boolean saveAssignment(UserFeatureAssignment assignment, UserFeatureAssignmentHistory history, boolean fallbackEnabled);
}
