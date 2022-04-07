package com.object.checker.service.interfaces;

import com.object.checker.model.marker.Marker;

public interface MarkerService {
    void savaData(Marker marker);

    String getLastMark();

    String getLastMark2();

    boolean isStarted();

    void getObjectFromOvhAndProcess();

    boolean isRunning();

    int getReadCount();
}
