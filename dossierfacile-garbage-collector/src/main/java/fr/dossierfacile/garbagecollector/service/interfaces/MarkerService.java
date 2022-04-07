package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.garbagecollector.model.marker.Marker;

public interface MarkerService {
    void savaData(Marker marker);

    String getLastMark();

    String getLastMark2();

    boolean isStarted();

    void getObjectFromOvhAndProcess();

    boolean isRunning();

    int getReadCount();
}
