package fr.dossierfacile.api.front.model.tenant;

import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;

public record ApplicationErrorResponse(ApplicationErrorCode code) {
}
