package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.process.file.barcode.InMemoryFile;

import java.util.Optional;

public abstract class QrCodeDocumentIssuer<REQUEST extends AuthenticationRequest> {

    protected abstract Optional<REQUEST> buildAuthenticationRequestFor(InMemoryFile file);

    protected abstract AuthenticationResult authenticate(InMemoryFile file, REQUEST authenticationRequest);

    public Optional<AuthenticationResult> tryToAuthenticate(InMemoryFile file) {
        return buildAuthenticationRequestFor(file).map(request -> authenticate(file, request));
    }

}