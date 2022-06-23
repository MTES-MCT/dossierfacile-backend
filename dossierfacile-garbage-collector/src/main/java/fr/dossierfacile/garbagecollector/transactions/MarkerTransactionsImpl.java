package fr.dossierfacile.garbagecollector.transactions;

import fr.dossierfacile.garbagecollector.model.marker.Marker;
import fr.dossierfacile.garbagecollector.repo.marker.MarkerRepository;
import fr.dossierfacile.garbagecollector.transactions.interfaces.MarkerTransactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarkerTransactionsImpl implements MarkerTransactions {
    private final MarkerRepository markerRepository;

    @Override
    @Transactional
    public void saveMarkerIfNotYetSaved(String name) {
        if (markerRepository.findByPath(name) == null) {
            Marker marker = new Marker();
            marker.setPath(name);
            markerRepository.saveAndFlush(marker);
        }
    }

    @Override
    @Transactional
    public void deleteAllMarkers() {
        markerRepository.deleteAll();
    }

    @Override
    @Transactional
    public void deleteMarker(Marker marker) {
        markerRepository.delete(marker);
    }
}
