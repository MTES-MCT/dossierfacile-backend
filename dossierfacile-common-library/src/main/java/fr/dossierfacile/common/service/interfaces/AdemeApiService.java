package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.exceptions.AdemeApiBadRequestException;
import fr.dossierfacile.common.exceptions.AdemeApiInternalServerErrorException;
import fr.dossierfacile.common.exceptions.AdemeApiNotFoundException;
import fr.dossierfacile.common.exceptions.AdemeApiUnauthorizedException;
import fr.dossierfacile.common.model.AdemeResultModel;

public interface AdemeApiService {
    AdemeResultModel getDpeDetails(String dpeNumber) throws AdemeApiInternalServerErrorException, AdemeApiBadRequestException, AdemeApiUnauthorizedException, AdemeApiNotFoundException, InterruptedException;
}
