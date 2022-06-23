package fr.dossierfacile.garbagecollector.transactions.interfaces;

import fr.dossierfacile.garbagecollector.model.marker.Marker;

public interface MarkerTransactions {
    void saveMarkerIfNotYetSaved(String name);
    void deleteAllMarkers();
    void deleteMarker(Marker marker);
}
