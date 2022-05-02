package fr.dossierfacile.garbagecollector.service.interfaces;

public interface MarkerService {
    boolean toggleScanner();
    boolean isRunning();
    void startScanner();
    void setRunningToFalse();
    void setRunningToTrue();
    void cleanDatabaseOfScanner();
}
