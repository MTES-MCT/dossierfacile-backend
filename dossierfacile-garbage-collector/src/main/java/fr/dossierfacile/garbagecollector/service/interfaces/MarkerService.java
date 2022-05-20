package fr.dossierfacile.garbagecollector.service.interfaces;

public interface MarkerService {
    boolean toggleScanner();
    boolean isRunning();
    boolean stoppingScanner();
    void stopScanner();
    void setRunningToTrue();
    void startScanner();
}
