package fr.dossierfacile.api.front.das.mesri;

public interface ApiParticulierClient {

    /**
     * Calls API Particulier in FranceConnect mode: the FranceConnect access token is the only credential,
     * API Particulier resolves the user's identity from it and queries the MESR.
     *
     * @throws FranceConnectTokenUnavailableException on 401 (token invalid or expired, code 51502)
     * @throws MesriStudentNotFoundException         on 404 (code 25003)
     * @throws MesriNoAdmissionException             on 200 without any admission
     * @throws ApiParticulierException               on any other status or network error
     */
    MesriStudentStatus fetchStudentStatus(String franceConnectAccessToken);
}
